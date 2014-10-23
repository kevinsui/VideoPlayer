import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;


public class VideoPlayer {
  static final int WIDTH = 352;
  static final int HEIGHT = 288;

  public static void main(String[] args) {
    // Get data from command line arguments
    String fileName = args[0];
   	double xScale = Double.parseDouble(args[1]);
    double yScale = Double.parseDouble(args[2]);
    int frameRate = Integer.parseInt(args[3]);
    int antiAliasing = Integer.parseInt(args[4]);
    int analysis = Integer.parseInt(args[5]);
    // create buffered image array to store every frame
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    // Read bytes from video file
    try {
      System.out.println("Reading video file contents...");
	    File file = new File(args[0]);
	    InputStream is = new FileInputStream(file);
      // Get length of file and create byte array
	    long len = file.length();
	    byte[] bytes = new byte[(int)len];
	    // Read all bytes from video file into byte array
	    int offset = 0;
      int numRead = 0;
      while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
        offset += numRead;
      }
      // Create an image for each frame
      System.out.println("Creating images for each frame...");
    	int index = 0;
      while (index+HEIGHT*WIDTH*2 < len) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
    			for (int x = 0; x < WIDTH; x++) {
    				byte r = bytes[index];
    				byte g = bytes[index+HEIGHT*WIDTH];
    				byte b = bytes[index+HEIGHT*WIDTH*2]; 
    				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    				image.setRGB(x,y,pix);
    				index++;
    			}
    		}
        BufferedImage scaledImage = null;
        if (analysis == 1) {
          if (xScale != 1 || yScale != 1) {
            scaledImage = getNonlinearMapping(image, xScale, yScale);
          } else {
            scaledImage = image;
          }
          if (antiAliasing == 1) {
            scaledImage = getAntiAliasing(scaledImage, 1, 1);
          }
        } else {
          if (antiAliasing == 1) {
            scaledImage = getAntiAliasing(image, xScale, yScale);
          } else if (xScale != 1 || yScale != 1) {
            scaledImage = getScaledImage(image, xScale, yScale);
          } else {
            scaledImage = image;
          }
        }
        images.add(scaledImage);
        index += WIDTH*HEIGHT*2;
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Create frame and label to display video
    System.out.println("Playing video...");
    JFrame frame = new JFrame();
    JLabel label = new JLabel(new ImageIcon(images.get(0)));
    frame.getContentPane().add(label, BorderLayout.CENTER);
    frame.pack();
    frame.setVisible(true);
    for (int i = 1; i < images.size(); i++) {
      label.setIcon(new ImageIcon(images.get(i)));
      try {
        Thread.sleep(1000/frameRate); 
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    System.out.println("Finished!");
  }

  public static BufferedImage getScaledImage(BufferedImage original, double xScale, double yScale) {
    int width = (int)((double)original.getWidth() * xScale);
    int height = (int)((double)original.getHeight() * yScale);
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      int yOrig = (int)((double)y / yScale);
      for (int x = 0; x < width; x++) {
        int xOrig = (int)((double)x / xScale);
        int pix = original.getRGB(xOrig, yOrig);
        image.setRGB(x, y, pix);
      }
    }
    return image;
  }

  public static BufferedImage getAntiAliasing (BufferedImage original, double xScale, double yScale) {
    int width = (int)((double)original.getWidth() * xScale);
    int height = (int)((double)original.getHeight() * yScale);
    BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      int yOrig = (int)((double)y / yScale);
      for (int x = 0; x < width; x++) {
        int xOrig = (int)((double)x / xScale);
        Color newColor = getFilteredColor(original, xOrig, yOrig);
        newImage.setRGB(x, y, newColor.getRGB());
      }
    }
    return newImage;
  }

  public static Color getFilteredColor (BufferedImage image, int x, int y) {
    int r = 0;
    int g = 0;
    int b = 0;
    int count = 0;
    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        if (x+i < 0 || x+i >= image.getWidth())
          continue;
        if (y+j < 0 || y+j >= image.getHeight())
          continue;
        r += new Color(image.getRGB(x+i, y+j)).getRed();
        g += new Color(image.getRGB(x+i, y+j)).getGreen();
        b += new Color(image.getRGB(x+i, y+j)).getBlue();
        count++;
      }
    }
    return new Color(r/count, g/count, b/count);
  }

  public static BufferedImage getNonlinearMapping (BufferedImage original, Double xScale, Double yScale) {
    int width = (int)((double)original.getWidth() * xScale);
    int height = (int)((double)original.getHeight() * yScale);
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    double ratio = ((double) WIDTH/(double) HEIGHT) / ((double) width/(double) height);
    if (ratio < 1) {
      // take percentage along width
      double centerRatio = 0.6*WIDTH/HEIGHT;
      int centerWidth = (int)(centerRatio * height);
      int segment1 = width/2 - centerWidth/2;
      int segment3 = width/2 + centerWidth/2;
      double xScaleCenter = (double)(centerWidth/(0.6*WIDTH));
      double xScaleSide = (double)(width-centerWidth) / (0.4*WIDTH);

      BufferedImage centerSegment = original.getSubimage(WIDTH/5, 0, 3*WIDTH/5, HEIGHT);
      BufferedImage rightSegment = original.getSubimage(4*WIDTH/5, 0, WIDTH/5, HEIGHT);

      for (int y = 0; y < height; y++) {
        for (int x = 0; x <= segment1; x++) {
          int xOrig = (int)((double)x / xScaleSide);
          int yOrig = (int)((double)y / yScale);
          int pix = original.getRGB(xOrig, yOrig);
          image.setRGB(x, y, pix);  
        }
        for (int x = 0; x < segment3 - segment1; x++) {
          int xOrig = (int)((double)x / xScaleCenter);
          int yOrig = (int)((double)y / yScale);
          int pix = centerSegment.getRGB(xOrig, yOrig);
          image.setRGB(x+segment1, y, pix);  
        }
        for (int x = 0; x < segment1; x++) {
          int xOrig = (int)((double)x / xScaleSide);
          int yOrig = (int)((double)y / yScale);
          if (xOrig >= rightSegment.getWidth())
            continue;
          int pix = rightSegment.getRGB(xOrig, yOrig);
          if (x+segment3 >= image.getWidth())
            continue;
          image.setRGB(x+segment3, y, pix);  
        }
      }
    } else if (ratio > 1) {
      // take percentage along height
      // take percentage along width
      double centerRatio = 0.6*HEIGHT/WIDTH;
      int centerHeight = (int)(centerRatio * width);
      int segment1 = height/2 - centerHeight/2;
      int segment3 = height/2 + centerHeight/2;
      double yScaleCenter = (double)(centerHeight/(0.6*HEIGHT));
      double yScaleSide = (double)(height-centerHeight) / (0.4*HEIGHT);

      BufferedImage centerSegment = original.getSubimage(0, HEIGHT/5, WIDTH, 3*HEIGHT/5);
      BufferedImage bottomSegment = original.getSubimage(0, 4*HEIGHT/5, WIDTH, HEIGHT/5);

      for (int x = 0; x < width; x++) {
        for (int y = 0; y <= segment1; y++) {
          int yOrig = (int)((double)y / yScaleSide);
          int xOrig = (int)((double)x / xScale);
          int pix = original.getRGB(xOrig, yOrig);
          image.setRGB(x, y, pix);  
        }
        for (int y = 0; y < segment3 - segment1; y++) {
          int yOrig = (int)((double)y / yScaleCenter);
          int xOrig = (int)((double)x / xScale);
          int pix = centerSegment.getRGB(xOrig, yOrig);
          image.setRGB(x, y+segment1, pix);  
        }
        for (int y = 0; y < segment1-1; y++) {
          int yOrig = (int)((double)y / yScaleSide);
          int xOrig = (int)((double)x / xScale);
          if (yOrig >= bottomSegment.getHeight())
            continue;
          int pix = bottomSegment.getRGB(xOrig, yOrig);
          if (y+segment3 >= image.getHeight())
            continue;
          image.setRGB(x, y+segment3, pix);  
        }
      }
    } else {
      image = getScaledImage(original, xScale, yScale);
    }
    return image;
  }
}