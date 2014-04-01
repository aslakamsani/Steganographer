import java.util.Scanner;

//allow user to choose from a menu
public class BitmapDriver{
    public static void main(String[] args){
        Scanner in = new Scanner(System.in);
        while(true){
            printMenu();  
            choose(in.nextInt());
        }
    }
    private static void printMenu(){
        System.out.println("\nBMP Viewer and Encoder by Amit Lakamsani");
        System.out.println("1: View BMP");
        System.out.println("2: View BMP Header");
        System.out.println("3: Print Color Data");
        System.out.println("4: Encode BMP");
        System.out.println("5: Decode BMP");
        System.out.println("6: Quit");
        System.out.print("Choose: ");
    }
    
    //execute correct routine for each option
    private static void choose(int choice){
        if(choice==6) System.exit(0);
        Scanner in2 = new Scanner(System.in);
        System.out.print("\nEnter file: ");
        BitmapUtil img = new BitmapUtil(in2.nextLine());
        switch(choice){
            case 1: img.drawBMP(); break;
            case 2: img.printBMPHeader(); break;
            case 3: img.printColorArray(); break;
            case 4: System.out.print("\nEncode with: ");
                    img.encodeBMP(in2.nextLine());
                    System.out.print("\nSave as: ");
                    img.saveBMP(in2.nextLine());
                    break;
            case 5: System.out.print("\nPrint into: ");
                    img.decodeBMP(in2.nextLine());
                    break;
        }
    }
}