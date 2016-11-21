
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class MyProgram {
	int width = 352;
	int height = 288;
	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage img, newimg;
	
	public void idct(double[][] result, double[][] input)
	{
		double cu, cv;
		for(int i = 0; i < height; i+=8)
		{
			for(int j = 0; j < width; j+=8)
			{
				for(int x = 0; x < 8; x++)
				{
					for(int y = 0; y < 8; y++)
					{
						result[x+i][y+j] = 0;
						for(int u = 0; u < 8; u++)
						{
							for(int v = 0; v < 8; v++)
							{
								if(u == 0)
									cu = 1/Math.sqrt(2);
								else
									cu = 1;
								if(v == 0)
									cv = 1/Math.sqrt(2);
								else
									cv = 1;
								result[x+i][y+j] += (cu * cv * input[u+i][v+j] * Math.cos(((2*x+1)*u*Math.PI)/16) * Math.cos(((2*y+1)*v*Math.PI)/16));
							}
						}
						result[x+i][y+j] *= 0.25;
					}
				}
			}
		}
	}
	
	public void dct(double[][] result, double[][] input, int n)
	{
		double cu, cv;
		for(int i = 0; i < height; i+=8)
		{
			for(int j = 0; j < width; j+=8)
			{
				for(int u = 0; u < 8; u++)
				{
					for(int v = 0; v < 8; v++)
					{
						if(u + v < n)
						{	
							if(u == 0)
								cu = 1/Math.sqrt(2);
							else
								cu = 1;
							if(v == 0)
								cv = 1/Math.sqrt(2);
							else
								cv = 1;
							double sum = 0;
							for(int x = 0; x < 8; x++)
							{
								for(int y = 0; y < 8; y++)
								{
									sum += (input[x+i][y+j] * Math.cos(((2*x+1)*u*Math.PI)/16) * Math.cos(((2*y+1)*v*Math.PI)/16));
								}
							}
							result[u+i][v+j] = sum * 0.25 * cu * cv;
						}
						else
							result[u+i][v+j] = 0;
					}
				}	
			}	
		}	
	}

	public void showIms(String[] args){
		double[][] ydct = new double[height][width];
		double[][] ymat = new double[height][width];
		double[][] prdct = new double[height][width];
		double[][] pr = new double[height][width];
		double[][] pbdct = new double[height][width];
		double[][] pb = new double[height][width];
		int n = Integer.parseInt(args[1]);
		double[][] convertMatrix = {{0.299,0.587,0.114},{-0.169,-0.331,0.500},{0.500,-0.419,-0.081}};
		double[][] invertMatrix = {{1,0,1.402},{1,-0.344,-0.714},{1,1.772,0}};
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		newimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		try {
			File file = new File(args[0]);
			InputStream is = new FileInputStream(file);
			long len = file.length();
			byte[] bytes = new byte[(int)len];

			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}

			int ind = 0;
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){
					//byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
			ind = 0;
			int[] rgb = new int[3];
			double[] yprpb = new double[3];
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){
					rgb[0] = bytes[ind]&0xff;
					rgb[1] = bytes[ind+height*width]&0xff;
					rgb[2] = bytes[ind+height*width*2]&0xff;
					ind++;
					for(int i = 0; i < 3; i++)
					{
						double sum = 0;
						for(int k = 0; k < 3; k++)
						{
							sum += convertMatrix[i][k]*rgb[k];
						}
						yprpb[i] = sum;
					}
					ymat[y][x] = yprpb[0];
					pr[y][x] = yprpb[1];
					pb[y][x] = yprpb[2];
				}
			}
			
			dct(ydct, ymat, n);
			dct(prdct, pr, n);
			dct(pbdct, pb, n);
			idct(ymat, ydct);
			idct(pr, prdct);
			idct(pb, pbdct);
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){
					yprpb[0] = ymat[y][x];
					yprpb[1] = pr[y][x];
					yprpb[2] = pb[y][x];
					for(int i = 0; i < 3; i++)
					{
						double sum = 0;
						for(int k = 0; k < 3; k++)
						{
							sum += invertMatrix[i][k]*yprpb[k];
						}
						rgb[i] = (int)sum;
					}
					int pix = (0xff000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
					newimg.setRGB(x,y,pix);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Use labels to display the images
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Image after modification (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel(new ImageIcon(img));
		lbIm2 = new JLabel(new ImageIcon(newimg));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		MyProgram ren = new MyProgram();
		ren.showIms(args);
	}
}