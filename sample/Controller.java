package sample;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import cipher.ThreeFish256;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Random;

public class Controller {

    // Will get initialized when we encrypt or decrypt.
    //
    ThreeFish256 cipher;

    @FXML
    Button encryptBtn;

    @FXML
    Button decryptBtn;

    @FXML
    TextField keyField1;

    @FXML
    TextField keyField2;

    @FXML
    TextField keyField3;

    @FXML
    TextField keyField4;

    @FXML
    TextField tweakField1;

    @FXML
    TextField tweakField2;

    @FXML
    Button browseBtn;

    @FXML
    TextField pathField;

    @FXML public void encryptHandler() {

        System.out.println("Encrypting...");

        File f = getPath();
        long[] key = getKey();
        long[] tweak = getTweak();

        if (f == null || key == null || tweak == null)
            return; // Bad input, just stop.


        Random rand = new Random(System.currentTimeMillis());

        cipher = new ThreeFish256(key, tweak);

        // Are we a multiple of 256 / 8 bytes?
        //
        long numBytes = f.length();

        // block size, in bytes!
        //
        long blockSize = 256 / 8;
        long remainder = numBytes % blockSize;
        long padding = blockSize - remainder;
        long numBlocks = numBytes / blockSize + 1;

        System.out.println("Padding: " + padding);
        long C[] = new long[4];
        // We generate a random block as the first block, but
        // for the first 8 bytes, we store a long telling us
        // how many bytes to throw out. including the first
        // block and the extra padding for the next block.
        //
        if (remainder == 0)
            // Okay, no extra padding.
            C[0] = 4 * 8; // each long in first block is 8 bytes.
        else {

            C[0] = 4 * 8 + padding;

            // Fill up rest of first block.
            for (int i = 1; i < 4; i++)
                C[i] = rand.nextLong();
        }

        byte curBlock[] = new byte[(int)blockSize];

        try (
                FileInputStream fr = new FileInputStream(f.getAbsolutePath());
                FileOutputStream fo = new FileOutputStream(f.getAbsoluteFile() + ".encry");
        ) {


            // Okay, we can start reading the file... block at a time, starting with
            // first special block, then we'll add padding next, then we proceed
            // as normal.
            //
            C = cipher.encrypt(C);

            fo.write(longToBytes(C));

            // Padding...
            int i, j;
            for (i = 0; i < (int)padding; i++)
                curBlock[i] = (byte) (i + 1);
            for (j = i; j < 32; j++)
                curBlock[j] = (byte)fr.read();

            long P[] = bytesToLong(curBlock);
            // CBC mode!
            //
            for (i = 0; i < 4; i++) {
                C[i] = P[i] ^ C[i];
            }
            C = cipher.encrypt(C);

            fo.write(longToBytes(C));

            // Now do the rest;
            for (i = 1; i < numBlocks; i++) {
                for (j = 0; j < 4 * 8; j++) {
                    curBlock[j] = (byte) fr.read();
                }

                // XOR with previously encrypted block
                //
                P = bytesToLong(curBlock);
                for (j = 0; j < 4; j++)
                    P[j] ^= C[j];

                // Encrypt it now!
                C = cipher.encrypt(P);

                // Write it out!
                //
                fo.write(longToBytes(C));
            }


        } catch (Exception e) {
            errMsg("Error reading file or writing file!\n" + e.toString());
        }

    }

    @FXML public void decryptHandler() {

        System.out.println("Decrypting....");
        File f = getPath();
        long[] key = getKey();
        long[] tweak = getTweak();

        if (f == null || key == null || tweak == null)
            return; // Bad input, just stop.


        cipher = new ThreeFish256(key, tweak);

        // We should always be a multiple of block size!
        //
        long numBytes = f.length();

        // block size, in bytes!
        //
        long blockSize = 256 / 8;
        long numBlocks = numBytes / blockSize;

        long C[];

        byte curBlock[] = new byte[(int)blockSize];

        try (
                FileInputStream fr = new FileInputStream(f.getAbsolutePath());
                FileOutputStream fo = new FileOutputStream(f.getAbsolutePath() + ".decry");
                FileOutputStream fo2 = new FileOutputStream(f.getParent() + "/out.txt")
        ) {

            int i, j;

            // First block is special, it tells us how much padding we have!
            //
            fr.read(curBlock);

            long P[];
            C = bytesToLong(curBlock);
            P = cipher.decrypt(C);

            int padding = (int)P[0] - 4 * 8;
            System.out.println("Padding: " + padding);

            // So decrypt the next block and ignore the first (padding) bytes!
            //
            byte PBytes[];
            fr.read(curBlock);
            long curBlockLong[] = bytesToLong(curBlock);
            curBlockLong = cipher.decrypt(curBlockLong);

            for (i = 0; i < 4; i++)
                P[i] = curBlockLong[i] ^ C[i];
            PBytes = longToBytes(P);
            for (i = (int)padding; i < 32; i++) {
                fo.write(PBytes[i]);
                fo2.write(PBytes[i]);
            }

            // Now we can write the rest as normal;
            for (i = 2; i < numBlocks; i++) {

                // Remember last encrypted text for
                // XOR.
                //
                C = bytesToLong(curBlock);
                fr.read(curBlock);

                curBlockLong = cipher.decrypt(bytesToLong(curBlock));

                for (j = 0; j < 4; j++)
                    P[j] = curBlockLong[j] ^ C[j];

                fo.write(longToBytes(P));
                fo2.write(longToBytes(P));
            }

        } catch (Exception e) {
            errMsg("Error reading file or writing file!\n" + e.toString());
        }
    }

    @FXML public void browseHandler () {

        pathField.setText(new FileChooser().showOpenDialog(new Stage()).getAbsolutePath());
    }

    private byte[] longToBytes(long[] longs) {

        ByteBuffer bf = ByteBuffer.allocate(longs.length * 8);
        for (int i = 0; i < longs.length; i++)
            bf.putLong(longs[i]);

        return bf.array();
    }

    // Convert the 32 bytes to 4 longs
    private long[] bytesToLong(byte bytes[]) {

        long longs[] = new long[4];
        for (int i = 0; i < 4; i++) {
            longs[i] = 0;
            for (int j = 0; j < 8; j++) {
                longs[i] <<= 8;
                longs[i] |= bytes[i * 8 + j] & 0xFF; // 0xFF for sign extension during type promotion (//.-)
            }
        }

        return longs;
    }

    private File getPath() {

        File file = null;
        try {
            file = new File(pathField.getText());

        } catch (Exception e) {
            System.out.println("Invalid File Path. \n" + e.toString());
        }

        return file;
    }

    private void errMsg(String message) {
        Stage dialogStage = new Stage();
        HBox box;
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setScene(
                new Scene(
                        box = new HBox(
                                new Label(message)
                        )
                )
        );

        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 40, 40, 40));
        dialogStage.show();
    }

    // Returns null on failure.
    //
    private long[] getTweak() {
        long tweak[] = new long[2];

        String t1 = tweakField1.getText();
        String t2 = tweakField2.getText();

        if (t1.length() != 8 || t2.length() != 8) {
            errMsg("Tweak size not 8 characters long!");
            return null;
        }

        try {
            String t1Hex = "", t2Hex = "";
            for (char ch : t1.toCharArray())
                t1Hex += getHex((byte)ch);
            for (char ch : t2.toCharArray())
                t2Hex += getHex((byte)ch);

            tweak[0] = Long.decode("0x" + t1Hex);
            tweak[1] = Long.decode("0x" + t2Hex);

        } catch (NumberFormatException ne) {
            errMsg("Illegal character for tweak.");
            return null;
        }

        return tweak;
    }

    private long[] getKey() {
        long key[] = new long[4];

        String t1 = keyField1.getText();
        String t2 = keyField2.getText();
        String t3 = keyField3.getText();
        String t4 = keyField4.getText();

        if (t1.length() != 8 || t2.length() != 8 ||
                t3.length() != 8 || t4.length() != 8) {
            errMsg("Key size not 8 characters long!");
            return null;
        }

        try {
            String t1Hex = "", t2Hex = "", t3Hex = "", t4Hex = "";
            for (char ch : t1.toCharArray())
                t1Hex += getHex((byte)ch);
            for (char ch : t2.toCharArray())
                t2Hex += getHex((byte)ch);
            for (char ch : t3.toCharArray())
                t3Hex += getHex((byte)ch);
            for (char ch : t4.toCharArray())
                t4Hex += getHex((byte)ch);

            key[0] = Long.decode("0x" + t1Hex);
            key[1] = Long.decode("0x" + t2Hex);
            key[2] = Long.decode("0x" + t3Hex);
            key[3] = Long.decode("0x" + t4Hex);

        } catch (NumberFormatException ne) {
            errMsg("Illegal character for key.\n" + ne.toString());
            return null;
        }

        return key;

    }

    // Given a byte, return a string in hex representation, without leading
    // 0x.
    //
    private String getHex(byte b) {

        char arr[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};

        return "" + arr[b >>> 4] + arr[b & 0x0F];
    }
}
