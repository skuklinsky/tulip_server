import java.io.*;

public class ReadWrite {

    public static void writeGlobalToFile(Global global, String filepath) {
        try {
            FileOutputStream f = new FileOutputStream(new File(filepath));
            ObjectOutputStream o = new ObjectOutputStream(f);

            o.writeObject(global);

            o.close();
            f.close();

            System.out.println("Successfully saved Global instance to file");

        } catch (IOException e) {
            System.out.println("Error saving Global instance: " + e.getMessage());
        }
    }

    public static Global readGlobalFromFile(String filepath) {
        try {
            FileInputStream fi = new FileInputStream(new File(filepath));
            ObjectInputStream oi = new ObjectInputStream(fi);

            Global global = (Global) oi.readObject();

            oi.close();
            fi.close();

            return global;

        } catch (IOException e) {
            System.out.println("Error reading Global instance from file: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading Global instance from file: " + e.getMessage());
        }

        return null;
    }

}
