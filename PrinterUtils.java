import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.io.*;
import javax.print.attribute.standard.MediaSize; // Add this line

public class PrinterUtils {

    public static void print(String content, MediaSizeName mediaSizeName, String printerName) {
        DocPrintJob printJob = null;
        PrintService selectedPrintService = null;

        // Look for the desired printer by name
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService printService : printServices) {
            if (printService.getName().equals(printerName)) {
                selectedPrintService = printService;
                break;
            }
        }

        if (selectedPrintService == null) {
            System.out.println("Printer not found: " + printerName);
            return;
        }

        try {
            printJob = selectedPrintService.createPrintJob();

            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(new ByteArrayInputStream(content.getBytes()), flavor, null);

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(mediaSizeName);

            printJob.print(doc, attributes);

            // Add the paper cutting command (ESC/POS command)
            byte[] cutCommand = {0x1D, 'V', 1};
            InputStream cutInputStream = new ByteArrayInputStream(cutCommand);
            Doc cutDoc = new SimpleDoc(cutInputStream, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
            printJob.print(cutDoc, null);

        } catch (PrintException e) {
            e.printStackTrace();
        }
    }
}
