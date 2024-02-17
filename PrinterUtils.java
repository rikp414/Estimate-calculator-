import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.io.*;

public class PrinterUtils {

    public static void print(String content, MediaSizeName mediaSizeName, String printerName) {
        DocPrintJob printJob = null;
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);

            // Find the desired printer by name
            PrintService targetPrinter = null;
            for (PrintService printService : printServices) {
                if (printService.getName().equalsIgnoreCase(printerName)) {
                    targetPrinter = printService;
                    break;
                }
            }

            if (targetPrinter != null) {
                printJob = targetPrinter.createPrintJob();
            } else {
                System.out.println("Printer not found: " + printerName);
                return;
            }

            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(new ByteArrayInputStream(content.getBytes()), flavor, null);

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(mediaSizeName);

            printJob.print(doc, attributes);
        } catch (PrintException e) {
            e.printStackTrace();
        }
    }
}
