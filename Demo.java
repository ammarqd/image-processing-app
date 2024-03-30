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
        "Logarithmic Function",
        "Power Law Function",
        "Random Lookup Table",
        "Bit plane Slicing",
        "Histogram Equalisation",
        "Averaging Convolution",
        "Weighted Average Convolution",
        "4-neighbour Laplacian",
        "8-neigbour Laplacian",
        "4-neighbour Laplacian Enhancement",
        "8-neighbour Laplacian Enhancement",
        "Roberts X",
        "Roberts Y",
        "Sobel X",
        "Sobel Y",
        "Salt and Pepper noise",
        "Min filter",
        "Max filter",
        "Midpoint filter",
        "Median filter",
        "Simple threshold",
        "Iterative threshold"
    };
 
    int opIndex;  //option index for 
    int lastOp;

    private BufferedImage bi, bi2, biFiltered;   // the input image saved as bi;//
    int w, h;

    private JFrame frame;
    private JComboBox choices;
    private Stack<BufferedImage> imageHistory;
    private Stack<Integer> indexHistory;
    private int dragStartX, dragStartY, dragEndX, dragEndY;
    private boolean isDragging = false;
    private boolean isUndoAction = false;
    private boolean roiSelected = false;

    private int[][][] roiMaskArray;
     
    public Demo() {
        try {
            bi = ImageIO.read(new File("Images/cateyes.png"));
            w = bi.getWidth();
            h = bi.getHeight();
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

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getX() < w && e.getY() < h) {
                    if (dragStartX != -1 && roiSelected) {
                            dragStartX = -1;
                            dragStartY = -1;
                            dragEndX = -1;
                            dragEndY = -1;
                            roiSelected = false;
                            drawROIMask(bi);
                            repaint();
                    } else {
                        dragStartX = e.getX();
                        dragStartY = e.getY();
                        isDragging = true;
                    }
                }
            }
        
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    isDragging = false;
                    roiSelected = true;
                    drawROIMask(bi);
                    repaint();
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    dragEndX = Math.max(0, Math.min(e.getX(), bi.getWidth()));
                    dragEndY = Math.max(0, Math.min(e.getY(), bi.getHeight()));
                    repaint();
                }
            }
        });
    }

    private boolean loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir"), "Images"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
            return false;
    
        File selectedFile = fileChooser.getSelectedFile();
        BufferedImage image;
        try {
            image = ImageIO.read(selectedFile);
            if (image == null) {
                JOptionPane.showMessageDialog(this, "Unsupported image format.");
                return false;
            }
        } catch (IOException e) {
            System.out.println("Image could not be read");
            e.printStackTrace();
            return false;
        }
    
        if (opIndex == 5 || opIndex == 6 || opIndex == 7 || opIndex == 8 || opIndex == 10 || opIndex == 11 || opIndex == 12) {
            if (image.getWidth() != bi.getWidth() || image.getHeight() != bi.getHeight()) {
                JOptionPane.showMessageDialog(this, "Please select an image with the same dimensions as the first image.");
                return false;
            }
            bi2 = image;
            ensureRGBType(); // Ensure RGB type before displaying the image
            displayImageInNewWindow(bi2);
        } else {
            bi = image;
            rescaleImageCheck();
            ensureRGBType();
            resetImageState();
        }
        return true;
    }    
    
    private void displayImageInNewWindow(BufferedImage image) {
        JFrame frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(image));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }
    
    private void rescaleImageCheck() {
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        if (bi.getWidth() > screenWidth / 2) {
            int newWidth = screenWidth / 2;
            int newHeight = (bi.getHeight() * newWidth) / bi.getWidth();
            Image scaledImage = bi.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();
            bi = biFiltered = resizedImage;
        }
    }
    
    private void ensureRGBType() {
        w = bi.getWidth();
        h = bi.getHeight();
        if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics big = bi2.getGraphics();
            big.drawImage(bi, 0, 0, null);
            biFiltered = bi = bi2;
        }
    }
    
    private void resetImageState() {
        imageHistory.clear();
        indexHistory.clear();
        choices.setSelectedIndex(opIndex = 0);
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        revalidate();
        frame.pack();
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(w*2, h);
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
 
    public void paint(Graphics g) {
        filterImage();
        g.drawImage(bi, 0, 0, null);
        g.drawImage(biFiltered, w, 0, null);
        
         if (dragStartX != -1) {
            int x = Math.min(dragStartX, dragEndX);
            int y = Math.min(dragStartY, dragEndY);
            int width = Math.abs(dragEndX - dragStartX);
            int height = Math.abs(dragEndY - dragStartY);
            
            g.setColor(new Color(0, 102, 204, 90));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(0, 102, 204, 180));
            g.drawRect(x, y, width, height);
        }
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
                if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                for (int c = 1; c < 4; c++) {
                    if (imageArray[x][y][0] == 255) {
                        imageArray[x][y][c] = operation.applyAsInt(imageArray[x][y][c]);
                    }
                }
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
                if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                for (int c = 1; c < 4; c++) {
                    if (imageArray[x][y][0] == 255) {
                        imageArray[x][y][c] = operation.applyAsInt(imageArray[x][y][c], imageArray2[x][y][c]);
                    }
                }
            }
        }
        return convertToBimage(imageArray);
    }

    private int inputPrompt(int min, int max) {
        int input = -1;
        boolean validInput = false;
    
        while (!validInput) {
            String inputStr = JOptionPane.showInputDialog(null, "Enter a value between " + min + " and " + max + ":");
            if (inputStr == null) {
                return -1;
            }
            try {
                input = Integer.parseInt(inputStr);
                if (input >= min && input <= max) {
                    validInput = true;
                } else {
                    JOptionPane.showMessageDialog(null, "Please enter a value between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number.");
            }
        }
        return input;
    }

    private double inputPrompt(double min, double max) {
        double input = -1;
        boolean validInput = false;
    
        while (!validInput) {
            String inputStr = JOptionPane.showInputDialog(null, "Enter a value between " + min + " and " + max + ":");
            if (inputStr == null) {
                return -1;
            }
            try {
                input = Double.parseDouble(inputStr);
                if (input >= min && input <= max) {
                    validInput = true;
                } else {
                    JOptionPane.showMessageDialog(null, "Please enter a value between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number.");
            }
        }
        return input;
    }
    

    public BufferedImage imageNegative(BufferedImage timg) {
        return applyOperation(timg, value -> 255 - value);
    }

    public BufferedImage imageRescale(BufferedImage timg) {
        double num = inputPrompt(0.0,10.0);
        if(num != -1) 
            return applyOperation(timg, value -> Math.max(0, Math.min((int) Math.round(value * num), 255)));
        return timg;
    }
    
    public BufferedImage imageShift(BufferedImage timg) {
        int num = inputPrompt(0,100);
        if(num != -1) 
            return applyOperation(timg, value -> Math.max(0, Math.min(value + num, 255)));
        return timg;
    }

    public BufferedImage imageShiftRescale(BufferedImage timg) {
        Random random = new Random();
        int num = inputPrompt(0,100);
        if(num != -1) 
            return applyOperation(timg, value -> (int) Math.round((random.nextInt(255) + value) * 0.5));
        return timg;
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

    public void drawROIMask(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        roiMaskArray = new int[width][height][1];

        int roiStartX = Math.min(dragStartX, dragEndX);
        int roiStartY = Math.min(dragStartY, dragEndY);
        int roiWidth = Math.abs(dragEndX - dragStartX);
        int roiHeight = Math.abs(dragEndY - dragStartY);
    
        int endX = roiStartX + roiWidth;
        int endY = roiStartY + roiHeight;
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                roiMaskArray[x][y][0] = (x >= roiStartX && x < endX && y >= roiStartY && y < endY) ? 255 : 0;
            }
        }
    }
    
    public BufferedImage LogarithmicFunction(BufferedImage timg) {
        double num = inputPrompt(0.0,10.0);
        if(num != -1) {
            return applyOperation(timg, value -> {
                double logValue = 1 * Math.log(1 + value); 
                int scaledValue = (int) (255 * logValue / Math.log(256));
                return Math.max(0, Math.min(scaledValue, 255));
            });
        }
        return timg;
    }
    
    public BufferedImage PowerLawFunction(BufferedImage timg) {
        double num = inputPrompt(0.0,10.0);
        if(num != -1) {
            return applyOperation(timg, value -> {
                double newValue = 1 * Math.pow(value, 1.1);
                return (int) Math.max(0, Math.min(newValue, 255));
            });
        }
        return timg;
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
        int k = inputPrompt(0,7);
        if(k != -1)
            return applyOperation(timg, value -> ((value >> k) & 1) * 255);
        return timg;
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

    public BufferedImage imageConvolution(BufferedImage timg, double[][] mask) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);

        int[][][] convolutedArray = new int[width][height][4];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int r = 0, g = 0, b = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                        r += mask[1 - s][1 - t] * imageArray[x + s][y + t][1];
                        g += mask[1 - s][1 - t] * imageArray[x + s][y + t][2];
                        b += mask[1 - s][1 - t] * imageArray[x + s][y + t][3];
                    }
                }
                if (imageArray[x][y][0] == 255) {
                    convolutedArray[x][y][1] = Math.min(255, Math.max(0, Math.abs(r)));
                    convolutedArray[x][y][2] = Math.min(255, Math.max(0, Math.abs(g)));
                    convolutedArray[x][y][3] = Math.min(255, Math.max(0, Math.abs(b)));
                }
                else {
                    convolutedArray[x][y][1] = imageArray[x][y][1];
                    convolutedArray[x][y][2] = imageArray[x][y][2];
                    convolutedArray[x][y][3] = imageArray[x][y][3];
                }
            }
        }
        return convertToBimage(convolutedArray);
    }   

    public BufferedImage averagingConvolution(BufferedImage bi) {
        double[][] mask = {{1.0/9, 1.0/9, 1.0/9}, {1.0/9, 1.0/9, 1.0/9}, {1.0/9, 1.0/9, 1.0/9}};
        return imageConvolution(bi, mask);
    }

    public BufferedImage weightedAveragingConvolution(BufferedImage bi) {
        double[][] mask = {{1.0/16, 1.0/8, 1.0/16}, {1.0/8, 1.0/4, 1.0/8}, {1.0/16, 1.0/8, 1.0/16}};
        return imageConvolution(bi, mask);
    }

    public BufferedImage fourNeighbourLaplacian(BufferedImage bi) {
        double[][] mask = {{0, -1, 0}, {-1, 4, -1}, {0, -1, 0}};
        return imageConvolution(bi, mask);
    }

    public BufferedImage eightNeighbourLaplacian(BufferedImage bi) {
        double[][] mask = {{-1, -1, -1}, {-1, 8, -1}, {-1, -1, -1}};
        return imageConvolution(bi, mask);
    }

    public BufferedImage fourNeighbourLaplacianEnhancement(BufferedImage bi) {
        double[][] mask = {{0, -1, 0}, {-1, 5, -1}, {0, -1, 0}};
        return imageConvolution(bi, mask);
    }

    public BufferedImage eightNeighbourLaplacianEnhancement(BufferedImage bi) {
        double[][] mask = {{-1, -1, -1}, {-1, 9, -1}, {-1, -1, -1}};
        return imageConvolution(bi, mask);
    }

    public BufferedImage robertsX(BufferedImage bi) {
        double[][] mask = {{0, 0, 0}, {0, -1, 0}, {0, 0, 1}};
        return imageConvolution(convertToGrayscale(bi), mask);
    }

    public BufferedImage robertsY(BufferedImage bi) {
        double[][] mask = {{0, 0, 0}, {0, 0, -1}, {0, 1, 0}};
        return imageConvolution(convertToGrayscale(bi), mask);
    }

    public BufferedImage sobelX(BufferedImage bi) {
        double[][] mask = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        return imageConvolution(convertToGrayscale(bi), mask);
    }

    public BufferedImage sobelY(BufferedImage bi) {
        double[][] mask = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
        return imageConvolution(convertToGrayscale(bi), mask);
    }

    public BufferedImage addSaltAndPepperNoise(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);          //  Convert the image to array

        Random random = new Random();
        double saltPercentage = 0.15;
        double pepperPercentage = 0.15;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!roiSelected || (roiSelected && roiMaskArray[x][y][0] == 255)) {
                    double randomValue = random.nextDouble();

                    if (randomValue < saltPercentage) {
                        for (int c = 1; c < 4; c++) {
                            imageArray[x][y][c] = 255;
                        }
                    } else if (randomValue > 1 - pepperPercentage) {
                        for (int c = 1; c < 4; c++) {
                            imageArray[x][y][c] = 0;
                        }
                    }
                }
            }
        }
        return convertToBimage(imageArray);
    }

    public BufferedImage minFilter(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);
        int[][][] imageArray2 = new int[width][height][4];

        int[] rWindow = new int[9]; 
        int[] gWindow = new int[9];
        int[] bWindow = new int[9];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1];
                        gWindow[k] = imageArray[x + s][y + t][2];
                        bWindow[k] = imageArray[x + s][y + t][3];
                        k++;
                    }
                }

                int minR = Arrays.stream(rWindow).min().getAsInt();
                int minG = Arrays.stream(gWindow).min().getAsInt();
                int minB = Arrays.stream(bWindow).min().getAsInt();

                if (imageArray[x][y][0] == 255) {
                    imageArray2[x][y][1] = minR;
                    imageArray2[x][y][2] = minG;
                    imageArray2[x][y][3] = minB;
                } else {
                    imageArray2[x][y][1] = imageArray[x][y][1];
                    imageArray2[x][y][2] = imageArray[x][y][2];
                    imageArray2[x][y][3] = imageArray[x][y][3];
                }
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage maxFilter(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);
        int[][][] imageArray2 = new int[width][height][4];
        int[] rWindow = new int[9]; 
        int[] gWindow = new int[9];
        int[] bWindow = new int[9];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1];
                        gWindow[k] = imageArray[x + s][y + t][2];
                        bWindow[k] = imageArray[x + s][y + t][3];
                        k++;
                    }
                }

                int maxR = Arrays.stream(rWindow).max().getAsInt();
                int maxG = Arrays.stream(gWindow).max().getAsInt();
                int maxB = Arrays.stream(bWindow).max().getAsInt();

                if (imageArray[x][y][0] == 255) {
                    imageArray2[x][y][1] = maxR;
                    imageArray2[x][y][2] = maxG;
                    imageArray2[x][y][3] = maxB;
                } else {
                    imageArray2[x][y][1] = imageArray[x][y][1];
                    imageArray2[x][y][2] = imageArray[x][y][2];
                    imageArray2[x][y][3] = imageArray[x][y][3];
                }
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage midpointFilter(BufferedImage timg) {
        int width = timg.getWidth();
        int height = timg.getHeight();

        int[][][] imageArray = convertToArray(timg);
        int[][][] imageArray2 = new int[width][height][4];

        int[] rWindow = new int[9];
        int[] gWindow = new int[9];
        int[] bWindow = new int[9];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1];
                        gWindow[k] = imageArray[x + s][y + t][2];
                        bWindow[k] = imageArray[x + s][y + t][3];
                        k++;
                    }
                }

                int midpointR = (Arrays.stream(rWindow).max().getAsInt() + Arrays.stream(rWindow).min().getAsInt()) / 2;
                int midpointG = (Arrays.stream(gWindow).max().getAsInt() + Arrays.stream(gWindow).min().getAsInt()) / 2;
                int midpointB = (Arrays.stream(bWindow).max().getAsInt() + Arrays.stream(bWindow).min().getAsInt()) / 2;

                if (imageArray[x][y][0] == 255) {
                    imageArray2[x][y][1] = midpointR;
                    imageArray2[x][y][2] = midpointG;
                    imageArray2[x][y][3] = midpointB;
                } else {
                    imageArray2[x][y][1] = imageArray[x][y][1];
                    imageArray2[x][y][2] = imageArray[x][y][2];
                    imageArray2[x][y][3] = imageArray[x][y][3];
                }
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage medianFilter(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        int[][][] imageArray = convertToArray(inputImage);
        int[][][] imageArray2 = new int[width][height][4];

        int[] rWindow = new int[9];
        int[] gWindow = new int[9];
        int[] bWindow = new int[9];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (roiSelected) imageArray[x][y][0] = roiMaskArray[x][y][0];
                int k = 0;
                for (int s = -1; s <= 1; s++) {
                    for (int t = -1; t <= 1; t++) {
                        rWindow[k] = imageArray[x + s][y + t][1];
                        gWindow[k] = imageArray[x + s][y + t][2];
                        bWindow[k] = imageArray[x + s][y + t][3];
                        k++;
                    }
                }

                Arrays.sort(rWindow);
                Arrays.sort(gWindow);
                Arrays.sort(bWindow);

                if (imageArray[x][y][0] == 255) {
                    imageArray2[x][y][1] = rWindow[4];
                    imageArray2[x][y][2] = gWindow[4];
                    imageArray2[x][y][3] = bWindow[4];
                } else {
                    imageArray2[x][y][1] = imageArray[x][y][1];
                    imageArray2[x][y][2] = imageArray[x][y][2];
                    imageArray2[x][y][3] = imageArray[x][y][3];
                }
            }
        }

        return convertToBimage(imageArray2);
    }

    public BufferedImage convertToGrayscale(BufferedImage image) {
        int[][][] imageArray = convertToArray(image);
        int width = imageArray.length;
        int height = imageArray[0].length;
    
        // Convert each pixel to grayscale
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int luminance = (int) (0.2126 * imageArray[x][y][1] + 0.7152 * imageArray[x][y][2] + 0.0722 * imageArray[x][y][3]);
                imageArray[x][y][1] = luminance;
                imageArray[x][y][2] = luminance;
                imageArray[x][y][3] = luminance;
            }
        }
    
        return convertToBimage(imageArray);
    }
    
    public BufferedImage simpleThreshold(BufferedImage timg) {
        int threshold = 100;
        return applyOperation(convertToGrayscale(timg), value -> value > threshold ? 255 : 0);
    }

    public BufferedImage iterativeThreshold(BufferedImage timg) {
        timg = convertToGrayscale(timg);
        int maxIterations = 100;
        double threshold = calculateInitialThreshold(timg);
    
        for (int i = 0; i < maxIterations; i++) {
            double newThreshold = calculateMeanThreshold(timg, threshold);
            if (Math.abs(newThreshold - threshold) < 0.5) {
                threshold = newThreshold;
                break;
            }
            threshold = newThreshold;
        }
    
        return applyThreshold(timg, (int) threshold);
    }
    
    private double calculateInitialThreshold(BufferedImage timg) {
        int[][] histogram = calculateHistogram(timg);
        double mean = calculateMean(histogram[0]);
        return mean;
    }
    
    private double calculateMeanThreshold(BufferedImage timg, double threshold) {
        int[][] histogram = calculateHistogram(timg);
        double mean1 = calculateMean(histogram[0], 0, (int) threshold);
        double mean2 = calculateMean(histogram[0], (int) threshold, histogram[0].length);
        return (mean1 + mean2) / 2.0;
    }
    
    private double calculateMean(int[] histogram) {
        double sum = 0;
        int totalPixels = 0;
        for (int i = 0; i < histogram.length; i++) {
            sum += i * histogram[i];
            totalPixels += histogram[i];
        }
        return sum / totalPixels;
    }
    
    private double calculateMean(int[] histogram, int start, int end) {
        double sum = 0;
        int totalPixels = 0;
        for (int i = start; i < end; i++) {
            sum += i * histogram[i];
            totalPixels += histogram[i];
        }
        return sum / totalPixels;
    }
    
    private BufferedImage applyThreshold(BufferedImage timg, int threshold) {
        return applyOperation(timg, value -> value > threshold ? 255 : 0);
    }
    
    //************************************
    //  You need to register your function here
    //************************************
    public void filterImage() {

        if (opIndex == lastOp) {
            return;
        }

        lastOp = opIndex;

        if (!roiSelected) {
            biFiltered = bi;
        }

        switch (opIndex) {
        case 0: biFiltered = bi; /* original */
                return; 
        case 1: biFiltered = imageNegative(biFiltered); /* Image Negative */
                return;
        case 2: biFiltered = imageRescale(biFiltered);
                return;
        case 3: biFiltered = imageShift(biFiltered);
                return;
        case 4: biFiltered = imageShiftRescale(biFiltered);
                return;
        case 5: if (loadImage()) biFiltered = imageAddition(biFiltered, bi2);
                return;
        case 6: if (loadImage()) biFiltered = imageSubtraction(biFiltered, bi2);
                return;
        case 7: if (loadImage()) biFiltered = imageMultiplication(biFiltered, bi2);
                return;
        case 8: if (loadImage()) biFiltered = imageDivision(biFiltered, bi2);
                return;
        case 9: biFiltered = imageBitwiseNOT(biFiltered);
                return;
        case 10: if (loadImage()) biFiltered = imageBitwiseAND(biFiltered, bi2);
                 return;        
        case 11: if (loadImage()) biFiltered = imageBitwiseOR(biFiltered, bi2);
                 return;    
        case 12: if (loadImage()) biFiltered = imageBitwiseXOR(biFiltered, bi2);
                 return;    
        case 13: biFiltered = LogarithmicFunction(biFiltered);
                 return;
        case 14: biFiltered = PowerLawFunction(biFiltered);
                 return;
        case 15: biFiltered = randomLookupTable(biFiltered);
                 return;
        case 16: biFiltered = bitPlaneSlicing(biFiltered);
                 return;
        case 17: biFiltered = histogramEqualisation(biFiltered);
                 return;
        case 18: biFiltered = averagingConvolution(biFiltered);
                 return;
        case 19: biFiltered = weightedAveragingConvolution(biFiltered);
                 return;      
        case 20: biFiltered = fourNeighbourLaplacian(biFiltered);
                 return;
        case 21: biFiltered = eightNeighbourLaplacian(biFiltered);
                 return;
        case 22: biFiltered = fourNeighbourLaplacianEnhancement(biFiltered);
                 return;
        case 23: biFiltered = eightNeighbourLaplacianEnhancement(biFiltered);
                 return;
        case 24: biFiltered = robertsX(biFiltered);
                 return;
        case 25: biFiltered = robertsY(biFiltered);
                 return;
        case 26: biFiltered = sobelX(biFiltered);
                 return;
        case 27: biFiltered = sobelY(biFiltered);
                 return;
        case 28: biFiltered = addSaltAndPepperNoise(biFiltered);
                 return;
        case 29: biFiltered = minFilter(biFiltered);
                 return;
        case 30: biFiltered = maxFilter(biFiltered);
                 return;
        case 31: biFiltered = midpointFilter(biFiltered);
                 return;
        case 32: biFiltered = medianFilter(biFiltered);
                 return;
        case 33: biFiltered = simpleThreshold(biFiltered);
                 return;
        case 34: biFiltered = iterativeThreshold(biFiltered);
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
        } else if (e.getActionCommand().equals("Load")) {
            loadImage();
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
                chooser.setCurrentDirectory(new File(System.getProperty("user.dir"), "Images"));
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
        JFrame frame = new JFrame("Image Processing Demo");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        Demo de = new Demo();
        frame.add("Center", de);
        JButton loadButton = new JButton("Load");
        loadButton.setActionCommand("Load");
        loadButton.addActionListener(de);
        JButton undoButton = new JButton("Undo");
        undoButton.setActionCommand("Undo");
        undoButton.addActionListener(de);
        JComboBox formats = new JComboBox(de.getFormats());
        formats.setActionCommand("Formats");
        formats.addActionListener(de);
        JPanel panel = new JPanel();
        panel.add(loadButton);
        panel.add(undoButton);
        panel.add(de.choices);
        panel.add(new JLabel("Save As"));
        panel.add(formats);
        frame.add("North", panel);
        frame.pack();
        frame.setVisible(true);
    }
}
