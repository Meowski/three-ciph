package cipher;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by Meowington on 8/17/2015.
 */
public class Encryptor {

    // Will get initialized when we encrypt or decrypt.
    //
    private ThreeFish cipher;

    // Block size, in bits!
    //
    public Encryptor(long[] keys, long[] tweak, int blockSize) {

        switch(blockSize) {
            case 256: cipher = new ThreeFish256(keys, tweak); break;
            case 512: cipher = new ThreeFish512(keys, tweak); break;
            case 1024: cipher = new ThreeFish1024(keys, tweak); break;
            default: throw new IllegalArgumentException("Incorrect block size, choices are 256, 512, and 1024.");
        }
    }

    // Give use a stream to read from, a stream to write to, and tell us how
    // many bytes the file we are reading from is.
    //
    public void encryptHandler(DataInputStream fr, DataOutputStream fw, long numBytes) throws Exception{

        if (numBytes == 0) {
            System.err.println("Empty file!");
            return;
        }

        //System.out.println("Encrypting...");
        Random rand = new Random(System.currentTimeMillis());

        // block size, in bytes!
        //
        int blockSize = cipher.getBlockSize() / 8;

        // in longs!
        //
        int numLongs = blockSize / Long.BYTES;
        long remainder = numBytes % blockSize;
        long padding = (blockSize - remainder) % blockSize;
        long numBlocks = (numBytes + padding) / blockSize;

        //System.out.println("\tPadding: " + (int)padding);
        long C[] = new long[numLongs];
        // We generate a random block as the first block, but
        // for the first 8 bytes, we store a long telling us
        // how many bytes to throw out. including the first
        // block and the extra padding for the next block.
        //
        if (remainder == 0)
            // Okay, no extra padding.
            C[0] = 0; // each long in first block is 8 bytes.
        else {

            C[0] = padding;

            // Fill up rest of first block.
            for (int i = 1; i < numLongs; i++)
                C[i] = rand.nextLong();
        }

        byte curBlock[] = new byte[blockSize];

        // Okay, we can start reading the file... block at a time, starting with
        // first special block, then we'll add padding next, then we proceed
        // as normal.
        //
        C = cipher.encrypt(C);

        try {

            for (long l : C)
                fw.writeLong(l);

            // Padding...
            int i, j;
            for (i = 0; i < (int) padding; i++)
                curBlock[i] = (byte) (i + 1);
            for (j = i; j < blockSize; j++)
                curBlock[j] = fr.readByte();

            long P[] = bytesToLong(curBlock);
            // CBC mode!
            //
            for (i = 0; i < numLongs; i++) {
                C[i] = P[i] ^ C[i];
            }
            C = cipher.encrypt(C);

            for (long l : C)
                fw.writeLong(l);

            // Now do the rest;
            for (i = 1; i < numBlocks; i++) {

                fr.read(curBlock);

                // XOR with previously encrypted block
                //
                P = bytesToLong(curBlock);
                for (j = 0; j < numLongs; j++)
                    P[j] ^= C[j];

                // Encrypt it it now!
                C = cipher.encrypt(P);

                // Write it out!
                //
                fw.write(longToBytes(C));
            }
        } catch (Exception e) {
            System.err.println("Error reading / writing to file!\n" + e.toString());
            throw e;
        }
    }

    public void decryptHandler(DataInputStream fr, DataOutputStream fw, long numBytes) {

        // System.out.println("Decrypting....");
        if (numBytes <= 0)
            System.err.printf("Empty file / bad number of bytes: %d\n", numBytes);

        // block size, in bytes!
        //
        int blockSize = cipher.getBlockSize() / 8;
        int numLongs = blockSize / 8;
        long numBlocks = numBytes / blockSize;

        long C[];

        byte curBlock[] = new byte[blockSize];

        try {

            int i, j;

            // First block is special, it tells us how much padding we have!
            //
            fr.read(curBlock);

            long P[];
            C = bytesToLong(curBlock);
            P = cipher.decrypt(C);

            int padding = ((int)P[0]) % blockSize;
            //System.out.println("Padding: " + padding);

            // So decrypt the next block and ignore the first (padding) bytes!
            //
            byte PBytes[];
            fr.read(curBlock);
            long curBlockLong[] = bytesToLong(curBlock);
            curBlockLong = cipher.decrypt(curBlockLong);

            for (i = 0; i < numLongs; i++)
                P[i] = curBlockLong[i] ^ C[i];
            PBytes = longToBytes(P);
            for (i = padding; i < blockSize; i++) {
                fw.writeByte(PBytes[i]);
            }

            // Now we can write the rest as normal;
            for (i = 2; i < numBlocks; i++) {

                // Remember last encrypted text for
                // XOR.
                //
                C = bytesToLong(curBlock);
                fr.read(curBlock);

                curBlockLong = cipher.decrypt(bytesToLong(curBlock));

                for (j = 0; j < numLongs; j++)
                    P[j] = curBlockLong[j] ^ C[j];

                fw.write(longToBytes(P));
            }

        } catch (Exception e) {
            System.err.println("Error decrypting!");
            System.err.println("Did you give the correct key?");
            System.err.println(e);
            e.printStackTrace();
            //errMsg("Error reading file or writing file!\n" + e.toString());
        }
    }

    private byte[] longToBytes(long[] longs) {

        ByteBuffer bf = ByteBuffer.allocate(longs.length * 8);
        for (int i = 0; i < longs.length; i++)
            bf.putLong(longs[i]);

        return bf.array();
    }

    // Convert the 32 bytes to 4 longs
    private long[] bytesToLong(byte bytes[]) {

        int numLongs = cipher.getBlockSize() / (Long.BYTES * 8);
        long longs[] = new long[numLongs];
        for (int i = 0; i < numLongs; i++) {
            longs[i] = 0;
            for (int j = 0; j < Long.BYTES; j++) {
                longs[i] <<= 8;
                longs[i] |= bytes[i * 8 + j] & 0xFF; // 0xFF for sign extension during type promotion (//.-)
            }
        }

        return longs;
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
