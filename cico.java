import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {

    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int numThreads) throws IOException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        if (originalImage == null) {
            throw new IOException("Formato de imagem não suportado ou arquivo inválido.");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage filteredImage = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        Thread[] threads = new Thread[numThreads];
        long[] changedPixelsByThread = new long[numThreads];
        long[] unchangedPixelsByThread = new long[numThreads];

        for (int t = 0; t < numThreads; t++) {
            int threadId = t;

            threads[t] = new Thread(() -> {
                int startY = threadId * height / numThreads;
                int endY = (threadId + 1) * height / numThreads;
                long changedPixels = 0;
                long unchangedPixels = 0;

                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {

                        int[] avgColor = calculateNeighborhoodAverage(originalImage, x, y, kernelSize);

                        int newRGB = (avgColor[0] << 16) | (avgColor[1] << 8) | avgColor[2];
                        int oldRGB = originalImage.getRGB(x, y) & 0x00FFFFFF;

                        filteredImage.setRGB(x, y, newRGB);

                        if (newRGB == oldRGB) {
                            unchangedPixels++;
                        } else {
                            changedPixels++;
                        }
                    }
                }

                changedPixelsByThread[threadId] = changedPixels;
                unchangedPixelsByThread[threadId] = unchangedPixels;
            });

            threads[t].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) { 
                e.printStackTrace();
            }
        }

        ImageIO.write(filteredImage, "jpg", new File(outputPath));

        long changedPixels = 0;
        long unchangedPixels = 0;
        for (int i = 0; i < numThreads; i++) {
            changedPixels += changedPixelsByThread[i];
            unchangedPixels += unchangedPixelsByThread[i];
        }

        System.out.println("Pixels alterados: " + changedPixels);
        System.out.println("Pixels inalterados: " + unchangedPixels);
    }

    private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;

        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        int pixelCount = 0;

        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int rgb = image.getRGB(x, y);

                    redSum += (rgb >> 16) & 0xFF;
                    greenSum += (rgb >> 8) & 0xFF;
                    blueSum += rgb & 0xFF;

                    pixelCount++;
                }
            }
        }

        return new int[]{
            (int) (redSum / pixelCount),
            (int) (greenSum / pixelCount),
            (int) (blueSum / pixelCount)
        };
    }

    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java ImageMeanFilter <input_file> <num_threads>");
            System.exit(1);
        }

        String inputFile = args[0];
        int numThreads;
        try {
            numThreads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Número de threads inválido: " + args[1]);
            System.exit(1);
            return;
        }

        if (numThreads < 2) {
            System.err.println("Número de threads deve ser >= 2");
            System.exit(1);
        }

        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, numThreads);
        } catch (IOException e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }
}
