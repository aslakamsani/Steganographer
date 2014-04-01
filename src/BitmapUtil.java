import java.io.*;
import java.awt.*;
import gpdraw.*;

public class BitmapUtil{
	//the following are all values in the BMP header, as defined by the BMP format
    short magic, na1, na2, colorPlane, bitsPerPixel;
    int size, dataStart, headerSize, width, height, birgb, dataSize, horRes, vertRes,
        numColors, impColors;
    
    //array of pixels in bitmap
    Color[][] map;

    BitmapUtil(String file){
        loadBMP(file);
    }
    
    //loads bitmap and saves file header
    public void loadBMP(String file){
        System.out.print("Loading... ");
        try{
            FileInputStream fIn = new FileInputStream(new File(file));
            DataInputStream dIn = new DataInputStream(fIn);
            //determine if file is a .bmp file
            magic = dIn.readShort();
            if(magic!=0x424D){
                System.out.println("\nNot a .bmp file.");
                System.exit(0);
            }
            //load header information
            size = endianSwitch(dIn.readInt());
            na1 = endianSwitch(dIn.readShort());
            na2 = endianSwitch(dIn.readShort());
            dataStart = endianSwitch(dIn.readInt());
            headerSize = endianSwitch(dIn.readInt());
            //determine if file is pre-Windows 3.0
            if(headerSize!=40){
                System.out.println("\nPre-Windows 3.0");
                System.exit(0);
            }
            width = endianSwitch(dIn.readInt());
            height = endianSwitch(dIn.readInt());
            map = new Color[height][width]; //create array of Color objects
            colorPlane = endianSwitch(dIn.readShort());
            bitsPerPixel = endianSwitch(dIn.readShort());
            birgb = endianSwitch(dIn.readInt());
            //determine if file is compressed
            if(birgb!=0){
                System.out.println("\nCompressed image.");
                System.exit(0);
            }
            dataSize = endianSwitch(dIn.readInt());
            horRes = endianSwitch(dIn.readInt());
            vertRes = endianSwitch(dIn.readInt());
            numColors = endianSwitch(dIn.readInt());
            impColors = endianSwitch(dIn.readInt());
            //load pixel data
            for(int r=height-1; r>=0; r--){
                for(int c=0; c<width; c++){
                    int blue = (int) dIn.readUnsignedByte();
                    int green = (int) dIn.readUnsignedByte();
                    int red = (int) dIn.readUnsignedByte();
                    map[r][c] = new Color(red,green,blue);
                }
                for(int i=0; i<width%4; i++) dIn.read();
            }
            System.out.println("Done.");
        }catch(IOException e){
            System.out.println("\nInvalid file.");
            System.out.println(e.getMessage());
        }
    }
    
    //save bitmap with a certain file name
    public void saveBMP(String file){
        System.out.print("Saving... ");
        try{
            FileOutputStream fOut = new FileOutputStream(new File(file));
            DataOutputStream dOut = new DataOutputStream(fOut);
            dOut.writeShort(magic);
            dOut.writeInt(endianSwitch(size));
            dOut.writeShort(endianSwitch(na1));
            dOut.writeShort(endianSwitch(na2));
            dOut.writeInt(endianSwitch(dataStart));
            dOut.writeInt(endianSwitch(headerSize));
            dOut.writeInt(endianSwitch(width));
            dOut.writeInt(endianSwitch(height));
            dOut.writeShort(endianSwitch(colorPlane));
            dOut.writeShort(endianSwitch(bitsPerPixel));
            dOut.writeInt(endianSwitch(birgb));
            dOut.writeInt(endianSwitch(dataSize));
            dOut.writeInt(endianSwitch(horRes));
            dOut.writeInt(endianSwitch(vertRes));
            dOut.writeInt(endianSwitch(numColors));
            dOut.writeInt(endianSwitch(impColors));
            for(int r=height-1; r>=0; r--){
                for(int c=0; c<width; c++){
                    Color p = map[r][c];
                    dOut.writeByte(p.getBlue());
                    dOut.writeByte(p.getGreen());
                    dOut.writeByte(p.getRed());
                }
                for(int i=0; i<width%4; i++) dOut.writeByte(0);
            }
            dOut.close();
            System.out.println("Done.");
        }catch(IOException e){
            System.out.println("Invalid file.");
            System.out.println(e.getMessage());
        }
    }
    
    //encode bitmap with message in a certain file
    public void encodeBMP(String file){
        System.out.print("Encoding... ");
        try{
        	//read in file
            File f = new File(file);
            if(f.length() > ((map.length*map[0].length*3)/4)){
                System.out.println("\nFile too large.");
                System.exit(0);
            }
            FileInputStream fIn = new FileInputStream(f);
            DataInputStream dIn = new DataInputStream(fIn);
            //shift bit to encode one byte per eight bitmap pixels
            int r = map.length-1, c = 0, g = 0, red = 0, green = 0, blue = 0;
            for(long i=f.length(); i>0; i--){
                byte b = (byte) dIn.read();
                for(int j=0; j<8; j++){
                    int curBit = ((b<<j)&(0x80))/128;
                    Color p = map[r][c];
                    red = p.getRed(); green = p.getGreen(); blue = p.getBlue();
                    switch(g){
                        case 0: red = detBit(p.getRed(),curBit); g++; break;
                        case 1: green = detBit(p.getGreen(),curBit);  g++; break;
                        case 2: blue = detBit(p.getBlue(),curBit); g=0;
                    }
                    map[r][c] = new Color(red,green,blue);
                    if(g==0) c++;
                    if(c>=map[r].length){r--; c = 0;}
                    if(r<0) r = 0;
                }
            }
            na1 = (short) r; na2 = (short) c;
            System.out.println("Done.");
        }catch(IOException e){
            System.out.println("\nInvalid file.");
            System.out.println(e+" "+e.getMessage());
        }
    }
    
    //change color as required by bit
    private int detBit(int color, int bit){
        if(color%2==0&&bit==0) return color;
        else if(color%2==0&&bit==1) return ++color;
        else if(color%2==1&&bit==0) return ++color;
        else if(color%2==1&&bit==1) return color;
        return 0;
    }
    
    //decode bitmap to extract hidden file
    public void decodeBMP(String file){
        System.out.print("Decoding... ");
        int r = map.length-1, c = 0, g = 0;
        String curByte = "";
        try{
        	//take in each pixel and reverse bit shift
            FileWriter scribe = new FileWriter(file);
            while(r>na1||c<=na2){
                for(int j=0; j<8; j++){
                    Color p = map[r][c];
                    switch(g){
                        case 0: curByte += p.getRed()&1; g++; break;
                        case 1: curByte += p.getGreen()&1;  g++; break;
                        case 2: curByte += p.getBlue()&1; g=0; c++;
                    }
                    if(c>=map[r].length){r--; c = 0;}
                    if(r<0) r = 0;
                }
                char n = (char) Byte.parseByte(curByte,2);
                scribe.write((int)n);
                curByte = "";
            }
            scribe.close();
            System.out.println("Done.");
        }catch(IOException e){
            System.out.println("\nInvalid File.");
            System.out.println(e+" "+e.getMessage());
        }
    }
    
    //prints bitmap header as read in
    public void printBMPHeader(){
        System.out.println("\nMagic number: "+
                           Integer.toHexString((int)magic).toUpperCase());
        System.out.println("Size: "+size+" bytes");
        System.out.println("App. spec. 1: "+na1);
        System.out.println("App. spec. 2: "+na2);
        System.out.println("Pixel data starts: "+dataStart+" bytes");
        System.out.println("Header size: "+headerSize+" bytes");
        System.out.println("Width: "+width+" pixels");
        System.out.println("Height: "+height+" pixels");
        System.out.println("Number of color planes: "+colorPlane);
        System.out.println("Number of bits per pixel: "+bitsPerPixel+" bits");
        System.out.println("BI_RGB: "+birgb);
        System.out.println("Size of pixel data: "+dataSize+" bytes");
        System.out.println("Horizontal resolution: "+horRes+" pixels");
        System.out.println("Vertical resolution: "+vertRes+" pixels");
        System.out.println("Number of colors: "+numColors);
        System.out.println("Important colors: "+impColors);
    }
    
    //prints color data of pixels in bitmap
    public void printColorArray(){
        System.out.println();
        for(int i=0; i<map.length; i++){
            for(int j=0; j<map[i].length; j++){
                int r = map[i][j].getRed();
                int b = map[i][j].getBlue();
                int g = map[i][j].getGreen();
                System.out.print("("+r+","+b+","+g+")\t");
            }
            System.out.println();
        }
        System.out.println();
    }
    
    //draws bitmap manually using gpdraw library
    public void drawBMP(){
        drawBMP(0,0); //draw centered image
    }
    public void drawBMP(int x,int y){
        SketchPad pad = new SketchPad(width,height,0);
        DrawingTool pen = new DrawingTool(pad);
        pen.up(); pen.move(x-(width/2),y-(height/2));
        //draw each pixel
        for(int i=height-1; i>=0; i--){
            pen.setDirection(0); pen.down();
            for(int j=0; j<width; j++){
                pen.setColor(map[i][j]);
                pen.forward(1);
            }
            pen.up(); pen.backward(width);
            pen.setDirection(90); pen.forward(1);
        }
    }
    
    //switch from big-endian to little-endian or vice-versa
    private short endianSwitch(short n){
        return (short) (((n&0xFF)<<8) + ((n&0xFF00)>>8));
    }
    private int endianSwitch(int n){
        return ((n&0xFF)<<24) + ((n&0xFF00)<<8) + ((n&0xFF0000)>>8) + ((n>>24)&0xFF);
    }
}