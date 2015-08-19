package sample;

import cipher.Encryptor;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {

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

    @FXML
    ChoiceBox<String> choiceBox;

    @FXML
    Label keyLabel;

    private ExecutorService executorService;

    // Before we create a thread, make sure we are not
    // already operating on this file.
    //
    private ArrayList<String> paths;

    public void initialize() {

        // Allow 5 threads to run at a time.
        //
        executorService = Executors.newFixedThreadPool(5);
        paths = new ArrayList<>(10);

        choiceBox.getSelectionModel().selectedIndexProperty().addListener(
                (e, old, n) -> {
                    if (n.intValue() == 0) {
                        keyLabel.setText("Key (8 characters each): ");
                        keyField1.setText("meowmeow");
                        keyField2.setText("meowmeow");
                        keyField3.setText("meowmeow");
                        keyField4.setText("meowmeow");
                    }
                    else if (n.intValue() == 1) {
                        keyLabel.setText("Key (16 characters each): ");
                        keyField1.setText("meowmeowmeowmeow");
                        keyField2.setText("meowmeowmeowmeow");
                        keyField3.setText("meowmeowmeowmeow");
                        keyField4.setText("meowmeowmeowmeow");
                    }
                    else {
                        keyLabel.setText("Key (32 characters each): ");
                        keyField1.setText("meowmeowmeowmeowmeowmeowmeowmeow");
                        keyField2.setText("meowmeowmeowmeowmeowmeowmeowmeow");
                        keyField3.setText("meowmeowmeowmeowmeowmeowmeowmeow");
                        keyField4.setText("meowmeowmeowmeowmeowmeowmeowmeow");
                    }
                }
        );
    }

    @FXML public void encryptHandler() {

        File fin = getPath();
        File fout = new File(fin.getParent() + "/encry_" + fin.getName());

        if (fin.isFile())
            encryptFile(fin, fout);
        else if (fin.isDirectory())
            encryptDir(fin);
    }

    private void encryptDir(File fin) {

        Path parent = fin.toPath();
        String getEncryptedParent =
                parent.getParent().toAbsolutePath().toString() + "\\encry_" +
                        parent.getFileName().toString();

        executorService.execute(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        Files.walkFileTree(parent,
                                new FileVisitor<Path>() {
                                    @Override
                                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                                        String path = parent.relativize(dir).toString();
                                        System.out.println("File path before: " + path);
                                        path = path.replaceAll("\\\\", "\\\\encry_");

                                        if (path.equals(""))
                                            path = getEncryptedParent;
                                        else
                                            path = getEncryptedParent + "\\encry_" + path;
                                        //System.out.println("File path after: " + getEncryptedParent + "\\" + path);

                                        Path p = Paths.get(path);
                                        Files.createDirectory(p);
                                        //System.out.println(dir.toString());
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                                        Path rel = parent.relativize(file);

                                        String pathOut = "encry_" + rel.toString().replaceAll("\\\\", "\\\\encry_");
                                        pathOut = getEncryptedParent + "\\" + pathOut;

                                        File fout = new File(pathOut);

                                        encryptFile(file.toFile(), fout);
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                        return FileVisitResult.CONTINUE;
                                    }
                                }
                        );
                    } catch (Exception e) {

                    }
                }
            }
        );
    }

    private void encryptFile(File fin, File fout) {

        executorService.execute(
                new Runnable() {
                    @Override
                    public void run() {

                        if (paths.contains(fin.getPath())) {
                            System.err.println("already working on file: " + fin.getPath());
                            return;
                        }
                        else
                            paths.add(fin.getPath());

                        System.out.println("Encrypting... " + fin.getName());

                        try {
                            DataInputStream fr = new DataInputStream(new FileInputStream(fin));
                            DataOutputStream fw = new DataOutputStream(new FileOutputStream(fout));

                            Encryptor encryptor = new Encryptor(getKey(), getTweak(), getBlockSize());
                            encryptor.encryptHandler(fr, fw, fin.length());

                            fr.close();
                            fw.close();

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(e);
                            return;
                        }

                        System.out.println("Encrypted! " + fin.getName());
                        paths.remove(fin.getPath());
                    }
                });
    }

    @FXML public void decryptHandler() {

        File fin = getPath();
        File fout = new File(fin.getParent() + "/encry_" + fin.getName());

        if (fin.isFile())
            decryptFile(fin, fout);
        else if (fin.isDirectory())
            decryptDir(fin);
    }

    private void decryptDir(File fin) {

        Path parent = fin.toPath();
        String getDecryptedParent =
                parent.getParent().toAbsolutePath().toString() + "\\decry_" +
                        parent.getFileName().toString();

        executorService.execute(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        Files.walkFileTree(parent,
                            new FileVisitor<Path>() {
                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                                    String path = parent.relativize(dir).toString();
                                    System.out.println("File path before: " + path);
                                    path = path.replaceAll("\\\\", "\\\\decry_");

                                    if (path.equals(""))
                                        path = getDecryptedParent;
                                    else
                                        path = getDecryptedParent + "\\decry_" + path;
                                    //System.out.println("File path after: " + getEncryptedParent + "\\" + path);

                                    Path p = Paths.get(path);
                                    Files.createDirectory(p);
                                    //System.out.println(dir.toString());
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                                    Path rel = parent.relativize(file);

                                    String pathOut = "decry_" + rel.toString().replaceAll("\\\\", "\\\\decry_");
                                    pathOut = getDecryptedParent + "\\" + pathOut;

                                    File fout = new File(pathOut);

                                    decryptFile(file.toFile(), fout);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                    return FileVisitResult.CONTINUE;
                                }
                            }
                        );
                    } catch (Exception e) {

                    }
                }
            }
        );
    }

    public void decryptFile(File fin, File fout) {
        executorService.execute(
                new Runnable() {
                    @Override
                    public void run() {

                        if (paths.contains(fin.getPath()))
                            return;
                        else
                            paths.add(fin.getPath());

                        System.out.println("Decrypting... " + fin.getName());

                        try {
                            DataInputStream fr = new DataInputStream(new FileInputStream(fin));
                            DataOutputStream fw = new DataOutputStream(new FileOutputStream(fout));

                            Encryptor encryptor = new Encryptor(getKey(), getTweak(), getBlockSize());
                            encryptor.decryptHandler(fr, fw, fin.length());

                            fr.close();
                            fw.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(e);
                            return;
                        }

                        System.out.println("Decrypted! " + fin.getName());
                        paths.remove(fin.getPath());

                    }
                }
        );
    }

    @FXML public void browseHandler () {

        pathField.setText(new FileChooser().showOpenDialog(new Stage()).getAbsolutePath());
    }

    @FXML public void dirHandler() {
        pathField.setText(new DirectoryChooser().showDialog(new Stage()).getAbsolutePath());
    }

    private int getBlockSize() {
        return Integer.parseInt(choiceBox.getValue());
    }

    private byte[] longToBytes(long[] longs) {

        ByteBuffer bf = ByteBuffer.allocate(longs.length * 8);
        for (int i = 0; i < longs.length; i++)
            bf.putLong(longs[i]);

        return bf.array();
    }

    // Convert the 32 bytes to 4 longs
    private long[] bytesToLong(byte bytes[]) {

        long longs[] = new long[bytes.length / 8];
        for (int i = 0; i < longs.length; i++) {
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

        String t1 = keyField1.getText();
        String t2 = keyField2.getText();
        String t3 = keyField3.getText();
        String t4 = keyField4.getText();


        int length = (getBlockSize() / 8 ) / 4;
        long key[];
        if (t1.length() != length || t2.length() != length ||
                t3.length() != length || t4.length() != length) {
            errMsg("Key size not 8 characters long!");
            return null;
        }

        try {

            ByteBuffer byteBuffer = ByteBuffer.allocate(getBlockSize() / 8);
            String t1Hex = "", t2Hex = "", t3Hex = "", t4Hex = "";
            for (char ch : t1.toCharArray())
                byteBuffer.put((byte)ch);
            for (char ch : t2.toCharArray())
                byteBuffer.put((byte)ch);
            for (char ch : t3.toCharArray())
                byteBuffer.put((byte)ch);
            for (char ch : t4.toCharArray())
                byteBuffer.put((byte)ch);

            key = bytesToLong(byteBuffer.array());

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
