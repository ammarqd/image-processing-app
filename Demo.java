import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;
import java.util.TreeSet;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
 
public class Demo extends Component implements ActionListener {
    
    //************************************
    // List of the options(Original, Negative); correspond to the cases:
    //************************************
  
    String descs[] = {
        "Original", 
        "Negative",
        "Rescale",
        "Shift", 
        "Shift and Rescale",
        "Addition",
        "Subtraction",
        "Multiplication",
        "Division",
        "Bitwise NOT",
        "Bitwise AND",
        "Bitwise OR",
        "Bitwise XOR",
        "ROI based operation",
        "Logarithmic Function",
        "Power Law Function",
        "Random Lookup Table",
        "Bit plane Slicing",
        "Histogram Equalisation",
        "Image Convolution",
        "Salt and Pepper noise",
        "Min filter",
        "Max filter",
        "Midpoint filter",
        "Median filter"
    };
 
    int opIndex;  //option index for 
    int lastOp;

    private BufferedImage bi, bi3, biFiltered;   // the input image saved as bi;//
    int w, h;

    private JComboBox choices;
    private Stack<BufferedImage> imageHistory;
    private Stack<Integer> indexHistory;
    private boolean isUndoAction = false;
     
    public Demo() {
        try {
            bi = ImageIO.read(new File("Images/cateyes.png"));
            bi3 = ImageIO.read(new File("Images/mask.png"));
            w = Math.min(bi.getWidth(null), bi3.getWidth(null));
            h = Math.min(bi.getHeight(null), bi3.getHeight(null));
            System.out.println(bi.getType());
            if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics big = bi2.getGraphics();
                big.drawImage(bi, 0, 0, null);
                biFiltered = bi = bi2;
            }
        } catch (IOException e) {      // deal with the situation that th image has problem;/
            System.out.println("Image could not be read");
            System.exit(1);
        }

        choices = new JComboBox(getDescriptions());
        choices.setActionCommand("SetFilter");
        choices.addActionListener(this);
        imageHistory = new Stack<>();
        indexHistory = new Stack<>();
    }                         
 
    public Dimension getPreferredSize() {
        return new Dimension(w*3, h);
    }
 

    String[] getDescriptions() {
        return descs;
    }

    // Return the formats sorted alphabetically and in lower case
    public String[] getFormats() {
        String[] formats = {"bmp","gif","jpeg","jpg","png"};
        TreeSet<String> formatSet = new TreeSet<String>();
        for (String s : formats) {
            formatSet.add(s.toLowerCase());
        }
        return formatSet.toArray(new String[0]);
    }
 

    void setOpIndex(int i) {
        opIndex = i;
    }
 
    public void paint(Graphics g) { //  Repaint will call this function so the image will change.
        filterImage();      
        g.drawImage(bi, 0, 0, null);
        g.drawImage(biFiltered, w, 0, null);
        g.drawImage(bi3, w*2, 0, null);
    }
 

    //************************************
    //  Convert the Buffered Image to Array
    //************************************
    private static int[][][] convertToArray(BufferedImage image){
      int width = image.getWidth();
      int height = image.getHeight();

      int[][][] result = new int[width][height][4];

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int p = image.getRGB(x,y);
            int a = (p>>24)&0xff;
            int r = (p>>16)&0xff;
            int g = (p>>8)&0xff;
            int b = p&0xff;

            result[x][y][0]=a;
            result[x][y][1]=r;
            result[x][y][2]=g;
            result[x][y][3]=b;
         }
      }
      return result;
    }

    //************************************
    //  Convert the  Array to BufferedImage
    //************************************
    public BufferedImage convertToBimage(int[][][] TmpArray){

        int width = TmpArray.length;
        int height = TmpArray[0].length;

        BufferedImage tmpimg=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);

        for(int y=0; y<height; y++){
            for(int x =0; x<width; x++){
                int a = TmpArray[x][y][0];
                int r = TmpArray[x][y][1];
                int g = TmpArray[x][y][2];
                int b = TmpArray[x][y][3];
                
                //set RGB value

                int p = (a<<24) | (r<<16) | (g<<8) | b;
                tmpimg.setRGB(x, y, p);

            }
        }
        return tmpimg;
    }

    public BufferedImage applyOperation(BufferedImage timg, IntUnaryOperator operation) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);          //  Convert the image to array

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                imageArray[x][y][1] = operation.applyAsInt(imageArray[x][y][1]); //r
                imageArray[x][y][2] = operation.applyAsInt(imageArray[x][y][2]); //g
                imageArray[x][y][3] = operation.applyAsInt(imageArray[x][y][3]); //b
            }
        }
        return convertToBimage(imageArray);
    }

    public BufferedImage applyOperation(BufferedImage timg, IntBinaryOperator operation, BufferedImage timg2) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);          //  Convert the image to array
        int[][][] imageArray2 = convertToArray(timg2);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                imageArray[x][y][1] = operation.applyAsInt(imageArray[x][y][1], imageArray2[x][y][1]);
                imageArray[x][y][2] = operation.applyAsInt(imageArray[x][y][2], imageArray2[x][y][2]);
                imageArray[x][y][3] = operation.applyAsInt(imageArray[x][y][3], imageArray2[x][y][3]);
            }
        }
        return convertToBimage(imageArray);
    }

    public BufferedImage imageNegative(BufferedImage timg) {
        return applyOperation(timg, value -> 255 - value);
    }

    public BufferedImage imageRescale(BufferedImage timg) {
        return applyOperation(timg, value -> Math.max(0, Math.min((int) Math.round(value * 2), 255)));
    }
    
    public BufferedImage imageShift(BufferedImage timg) {
        return applyOperation(timg, value -> Math.max(0, Math.min(value + 100, 255)));
    }

    public BufferedImage imageShiftRescale(BufferedImage timg) {
        Random random = new Random();
        return applyOperation(timg, value -> (int) Math.round((random.nextInt(255) + value) * 0.5));
    }

    public BufferedImage imageBitwiseNOT(BufferedImage timg) {
        return applyOperation(timg, value -> ~value & 0xFF);
    }

    public BufferedImage imageAddition(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> Math.max(0, Math.min(value1 + value2, 255)), timg2);
    }

    public BufferedImage imageSubtraction(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> Math.max(0, Math.min(value1 - value2, 255)), timg2);
    }

    public BufferedImage imageMultiplication(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> (value1 * value2)/255, timg2);
    }
    
    public BufferedImage imageDivision(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> {
            if (value2 == 0) return 255; // Avoid division by zero
            int result = (int) Math.round((double) value1 / value2 * 255);
            return Math.max(0, Math.min(result, 255));
        }, timg2);
    }

    public BufferedImage imageBitwiseAND(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> (value1 & value2), timg2);
    }

    public BufferedImage imageBitwiseOR(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> (value1 | value2), timg2);
    }

    public BufferedImage imageBitwiseXOR(BufferedImage timg, BufferedImage timg2) {
        return applyOperation(timg, (value1, value2) -> (value1 ^ value2), timg2);
    }

    public BufferedImage roiBasedOperation(BufferedImage mainImage, BufferedImage alphaChannel) {
        int width = mainImage.getWidth();
        int height = mainImage.getHeight();
        
        int[][][] mainImageArray = convertToArray(mainImage);
        int[][][] alphaChannelArray = convertToArray(alphaChannel);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mainImageArray[x][y][0] = alphaChannelArray[x][y][1];
                mainImageArray[x][y][1] ^= mainImageArray[x][y][0];
                mainImageArray[x][y][2] ^= mainImageArray[x][y][0];
                mainImageArray[x][y][3] ^= mainImageArray[x][y][0];
            }
        }
        return convertToBimage(mainImageArray);
    }    

    public BufferedImage LogarithmicFunction(BufferedImage timg) {
        return applyOperation(timg, value -> {
            double logValue = 1 * Math.log(1 + value); 
            int scaledValue = (int) (255 * logValue / Math.log(256));
            return Math.max(0, Math.min(scaledValue, 255));
        });
    }
    
    public BufferedImage PowerLawFunction(BufferedImage timg) {
        return applyOperation(timg, value -> {
            double newValue = 1 * Math.pow(value, 1.1);
            return (int) Math.max(0, Math.min(newValue, 255));
        });
    }

    public BufferedImage randomLookupTable(BufferedImage timg) {
        int[] lookupTable =  new int[256];
        Random random = new Random();
        for (int i = 0; i < 256; i++) {
            lookupTable[i] = random.nextInt(256); 
        }
        return applyOperation(timg, pixelValue -> lookupTable[pixelValue]);
    }

    public BufferedImage bitPlaneSlicing(BufferedImage timg) {
        int k = 7; 
        return applyOperation(timg, value -> ((value >> k) & 1) * 255);
    }    

    public int[][] calculateHistogram(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();
        int[][] histogram = new int[3][256]; // Separate histograms for R, G, B
    
        int[][][] imageArray = convertToArray(timg);
    
        // Calculate histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                histogram[0][imageArray[x][y][1]]++; // Red
                histogram[1][imageArray[x][y][2]]++; // Green
                histogram[2][imageArray[x][y][3]]++; // Blue
            }
        }
        return histogram;
    }
    
    public double[][] normaliseHistogram(int[][] histogram, int totalPixels) {
        double[][] normalisedHistogram = new double[3][256];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 256; j++) {
                normalisedHistogram[i][j] = (double) histogram[i][j] / totalPixels;
            }
        }
        return normalisedHistogram;
    }
    
    public BufferedImage histogramEqualisation(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();
        int totalPixels = width * height;
    
        int[][] histogram = calculateHistogram(timg);
        double[][] normalizedHistogram = normaliseHistogram(histogram, totalPixels);
    
        // Calculate cumulative distribution
        double[][] cumulativeDistribution = new double[3][256];
        for (int i = 0; i < 3; i++) {
            cumulativeDistribution[i][0] = normalizedHistogram[i][0];
            for (int j = 1; j < 256; j++) {
                cumulativeDistribution[i][j] = cumulativeDistribution[i][j - 1] + normalizedHistogram[i][j];
            }
        }
        return applyOperation(timg, value -> (int) Math.round(cumulativeDistribution[0][value] * 255));
    }

    public BufferedImage imageConvolution(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);
        double[][] mask = {{0, -1, 0}, {-1, 5, -1}, {0, -1, 0}};

        int[][][] convolutedArray = new int[width][height][4];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int r = 0, g = 0, b = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        r += mask[1 - s][1 - t] * imageArray[x + s][y + t][1];
                        g += mask[1 - s][1 - t] * imageArray[x + s][y + t][2];
                        b += mask[1 - s][1 - t] * imageArray[x + s][y + t][3];
                    }
                }
                convolutedArray[x][y][1] = Math.min(255, Math.max(0, Math.abs(r)));
                convolutedArray[x][y][2] = Math.min(255, Math.max(0, Math.abs(g)));
                convolutedArray[x][y][3] = Math.min(255, Math.max(0, Math.abs(b)));
            }
        }
        return convertToBimage(convolutedArray);
    }   

    public BufferedImage addSaltAndPepperNoise(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();
    
        int[][][] imageArray = convertToArray(timg); // Convert the image to array
    
        Random random = new Random();
    
        // Define the probabilities for salt and pepper noise
        double saltProbability = 0.05; // Adjust this value based on the desired noise level
        double pepperProbability = 0.05; // Adjust this value based on the desired noise level
    
        // Calculate the number of pixels to add noise to
        int numSalt = (int) (saltProbability * width * height);
        int numPepper = (int) (pepperProbability * width * height);
    
        // Add salt noise
        for (int i = 0; i < numSalt; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            imageArray[x][y][1] = 255; // Set R channel to 255 (pure white)
            imageArray[x][y][2] = 255; // Set G channel to 255 (pure white)
            imageArray[x][y][3] = 255; // Set B channel to 255 (pure white)
        }
    
        // Add pepper noise
        for (int i = 0; i < numPepper; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            imageArray[x][y][1] = 0; // Set R channel to 0 (pure black)
            imageArray[x][y][2] = 0; // Set G channel to 0 (pure black)
            imageArray[x][y][3] = 0; // Set B channel to 0 (pure black)
        }
    
        return convertToBimage(imageArray);
    }    

    public BufferedImage minFilter(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);
        int[][][] imageArray2 = new int[width][height][4]; // Resultant image array

        int[] rWindow = new int[9]; // Red channel window
        int[] gWindow = new int[9]; // Green channel window
        int[] bWindow = new int[9]; // Blue channel window

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1]; // Red channel
                        gWindow[k] = imageArray[x + s][y + t][2]; // Green channel
                        bWindow[k] = imageArray[x + s][y + t][3]; // Blue channel
                        k++;
                    }
                }

                int minR = Arrays.stream(rWindow).min().getAsInt();
                int minG = Arrays.stream(gWindow).min().getAsInt();
                int minB = Arrays.stream(bWindow).min().getAsInt();

                imageArray2[x][y][1] = minR;
                imageArray2[x][y][2] = minG;
                imageArray2[x][y][3] = minB;
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage maxFilter(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg); // Convert the input image to array
        int[][][] imageArray2 = new int[width][height][4]; // Resultant image array

        int[] rWindow = new int[9]; // Red channel window
        int[] gWindow = new int[9]; // Green channel window
        int[] bWindow = new int[9]; // Blue channel window

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1]; // Red channel
                        gWindow[k] = imageArray[x + s][y + t][2]; // Green channel
                        bWindow[k] = imageArray[x + s][y + t][3]; // Blue channel
                        k++;
                    }
                }

                int maxR = Arrays.stream(rWindow).max().getAsInt();
                int maxG = Arrays.stream(gWindow).max().getAsInt();
                int maxB = Arrays.stream(bWindow).max().getAsInt();

                imageArray2[x][y][1] = maxR;
                imageArray2[x][y][2] = maxG;
                imageArray2[x][y][3] = maxB;
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage midpointFilter(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg); // Convert the input image to array
        int[][][] imageArray2 = new int[width][height][4]; // Resultant image array

        int[] rWindow = new int[9]; // Red channel window
        int[] gWindow = new int[9]; // Green channel window
        int[] bWindow = new int[9]; // Blue channel window

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1]; // Red channel
                        gWindow[k] = imageArray[x + s][y + t][2]; // Green channel
                        bWindow[k] = imageArray[x + s][y + t][3]; // Blue channel
                        k++;
                    }
                }

                int midpointR = (Arrays.stream(rWindow).max().getAsInt() + Arrays.stream(rWindow).min().getAsInt()) / 2;
                int midpointG = (Arrays.stream(gWindow).max().getAsInt() + Arrays.stream(gWindow).min().getAsInt()) / 2;
                int midpointB = (Arrays.stream(bWindow).max().getAsInt() + Arrays.stream(bWindow).min().getAsInt()) / 2;

                imageArray2[x][y][1] = midpointR;
                imageArray2[x][y][2] = midpointG;
                imageArray2[x][y][3] = midpointB;
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage medianFilter(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        int[][][] imageArray = convertToArray(inputImage); // Convert the input image to array
        int[][][] imageArray2 = new int[width][height][4]; // Resultant image array

        int[] rWindow = new int[9]; // Red channel window
        int[] gWindow = new int[9]; // Green channel window
        int[] bWindow = new int[9]; // Blue channel window

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1]; // Red channel
                        gWindow[k] = imageArray[x + s][y + t][2]; // Green channel
                        bWindow[k] = imageArray[x + s][y + t][3]; // Blue channel
                        k++;
                    }
                }

                Arrays.sort(rWindow);
                Arrays.sort(gWindow);
                Arrays.sort(bWindow);

                imageArray2[x][y][1] = rWindow[4]; // Red channel
                imageArray2[x][y][2] = gWindow[4]; // Green channel
                imageArray2[x][y][3] = bWindow[4]; // Blue channel
            }
        }

        return convertToBimage(imageArray2); // Convert the resultant array back to BufferedImage
    }


    //************************************
    //  You need to register your function here
    //************************************
    public void filterImage() {

        if (opIndex == lastOp) {
            return;
        }

        lastOp = opIndex;

        switch (opIndex) {
        case 0: biFiltered = bi; /* original */
                return; 
        case 1: biFiltered = imageNegative(bi); /* Image Negative */
                return;
        case 2: biFiltered = imageRescale(bi);
                return;
        case 3: biFiltered = imageShift(bi);
                return;
        case 4: biFiltered = imageShiftRescale(bi);
                return;
        case 5 :biFiltered = imageAddition(bi, bi3);
                return;
        case 6: biFiltered = imageSubtraction(bi, bi3);
                return;
        case 7: biFiltered = imageMultiplication(bi, bi3);
                return;
        case 8: biFiltered = imageDivision(bi, bi3);
                return;
        case 9: biFiltered = imageBitwiseNOT(bi);
                return;
        case 10: biFiltered = imageBitwiseAND(bi, bi3);
                return;        
        case 11: biFiltered = imageBitwiseOR(bi, bi3);
                return;    
        case 12: biFiltered = imageBitwiseXOR(bi, bi3);
                return;    
        case 13: biFiltered = roiBasedOperation(bi, bi3);
                return;
        case 14: biFiltered = LogarithmicFunction(bi);
                return;
        case 15: biFiltered = PowerLawFunction(bi);
                return;
        case 16: biFiltered = randomLookupTable(bi);
                return;
        case 17: biFiltered = bitPlaneSlicing(bi);
                return;
        case 18: biFiltered = histogramEqualisation(bi);
                return;
        case 19: biFiltered = imageConvolution(bi);
                return;
        case 20: biFiltered = addSaltAndPepperNoise(bi);
                return;
        case 21: biFiltered = minFilter(bi);
                return;
        case 22: biFiltered = maxFilter(bi);
                return;
        case 23: biFiltered = midpointFilter(bi);
                return;
        case 24: biFiltered = medianFilter(bi);
                return;
        }

    }
 
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Undo")) {
            if (!imageHistory.isEmpty()) {
                isUndoAction = true;
                biFiltered = imageHistory.pop();
                choices.setSelectedIndex(lastOp = indexHistory.pop());
                isUndoAction = false;
            }
        } else {
            JComboBox cb = (JComboBox) e.getSource();
            if (cb.getActionCommand().equals("SetFilter")) {
                // If not an undo action, push the current state to history
                if (!isUndoAction) {
                    imageHistory.push(biFiltered);
                    indexHistory.push(opIndex);
                }
                setOpIndex(cb.getSelectedIndex());
                repaint();
            } else if (cb.getActionCommand().equals("Formats")) {
                String format = (String) cb.getSelectedItem();
                File saveFile = new File("savedimage." + format);
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(saveFile);
                int rval = chooser.showSaveDialog(cb);
                if (rval == JFileChooser.APPROVE_OPTION) {
                    saveFile = chooser.getSelectedFile();
                    try {
                        ImageIO.write(biFiltered, format, saveFile);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
 
    public static void main(String s[]) {
        JFrame f = new JFrame("Image Processing Demo");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        Demo de = new Demo();
        f.add("Center", de);
        JButton undoButton = new JButton("Undo");
        undoButton.setActionCommand("Undo");
        undoButton.addActionListener(de);
        JComboBox formats = new JComboBox(de.getFormats());
        formats.setActionCommand("Formats");
        formats.addActionListener(de);
        JPanel panel = new JPanel();
        panel.add(undoButton);
        panel.add(de.choices);
        panel.add(new JLabel("Save As"));
        panel.add(formats);
        f.add("North", panel);
        f.pack();
        f.setVisible(true);
    }
}
