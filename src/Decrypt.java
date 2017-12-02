import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class Decrypt {

	public static void main(String[] args){
		Path location = Paths.get(args[0]);
		try {
			byte[] data = Files.readAllBytes(location);
			if(data.length % 16 != 0){
				System.out.println("the received that is not multiple of 16");
				return;
			}
			byte[] dec = decryptInput(data);
			System.out.print("\033[H\033[2J");
			System.out.flush();
			System.out.println("\n\nThe answer I could derive is(byte representation):\n" + Arrays.toString(dec));
			System.out.println("\n\nThe answer I could derive is(with UTF-8 encoding):\n" + new String(dec,"UTF-8"));
		} catch (IOException e) {
			System.out.println("Cannot read the bytes");
			e.printStackTrace();
		}

	}
	
	private static byte[] randomBlock;
	private static Path oracle;

	public static byte[] decryptInput(byte[] data){
		oracle = Paths.get("oracle");
		int numBlocks = data.length /16;
		System.out.println("The number of blocks is " + numBlocks);
		ArrayList<byte[]> answer = new ArrayList<>();
		
		for(int currBlock = numBlocks; currBlock > 1; currBlock--){
			//prepare the blocks to be passed to the decryptBlock function
			byte[] last = new byte[16];
			byte[] secondLast = new byte[16];
			for(int k = 0; k < 16; k++){
				last[k] = data[(currBlock-1)*16+k];
			}
			for(int k = 0; k < 16; k++){
				secondLast[k] = data[(currBlock-2)*16+k];
			}
			//decrypt the block and add to the array with answers
			byte[] a = decryptBlock(secondLast,last);
			answer.add(a);			
		}
		
		byte[] finalAnswer = new byte[answer.size()*answer.get(0).length];
		//reverse the arraylist in the right order and transfer the data into an array
		Collections.reverse(answer);
		int index = 0;
		for(byte[] a :  answer){
			for(int i = 0; i<a.length;i++){
				finalAnswer[index] = a[i];
				index++;
			}
		}
		
		return finalAnswer;
	}
	
	private static byte[] decryptBlock(byte[] secondLast, byte[] last){
		//decrypt  the last byte of the block
		byte[] lastByte = decryptLastByte(secondLast, last);
		
		//Store all the found keys
		ArrayList<Byte> keys =  new ArrayList<>();
		keys.add(lastByte[1]);
				
		//Store  all the decrypted bytes
		ArrayList<Byte> bytes = new ArrayList<>();
		bytes.add(lastByte[0]);
		
		//work from the end to the beginning
		for(int k = 14; k>= 0;k--){
			
			//copy the random bytes back
			byte[] random = new byte[k+1];
			for(int m = k; m>= 0; m--){
				random[m] = randomBlock[m];
			}
			
			//set the last byte to 0
			random[k] = 0;
			
			//generate the second part of the sequence - key XOR (17 - k)
			ArrayList<Byte>	keyDuplicate = new ArrayList<>();
			
			
			System.out.print("The key sequence is ");
			for(Byte b: keys){
				System.out.print(Arrays.toString(new byte[]{(byte) b})+ ", ");
			}
			System.out.println();
			
			
			
			for(int n = 0; n < keys.size(); n++){
				System.out.println("The key before modification is " + Arrays.toString(new byte[]{(byte) (keys.get(n))}));
				System.out.println("The modificator(should be 2,3,4,5) is 17-k+1 = " + (17-k-1));
				System.out.println("The key I  modified is " + Arrays.toString(new byte[]{(byte) (keys.get(n) ^ (17 - k - 1))}));
				keyDuplicate.add((byte) (keys.get(n) ^ (17 - k - 1)));
			}
			System.out.println();
			
			
			byte[] randomWithKeys = concatenate(random,convertByteList(keyDuplicate));
			System.out.println("The randomBlock + the keys is "+ Arrays.toString(randomWithKeys));
			
			//increment until the oracle returns a positive answer
			byte[] test = concatenate(randomWithKeys,last);
			
			while(!checkOracle(test)){
				System.out.println("DecryptBlock "+test[k]);
				test[k] += 1;
			}
			
			//add  the key to the rest of the keys and reverse so that it is at the beginning
			byte key = (byte) (test[k] ^ (17 - k - 1));
			Collections.reverse(keys);
			keys.add(key);
			System.out.println("\nI am adding the following key to the block: " + Arrays.toString(new byte[]{(key)}));
			Collections.reverse(keys);
			
			//add the decrypted byte to the rest of the bytes and reverse to maintain the order
			bytes.add((byte) (secondLast[k] ^ key));
		}
		
		Collections.reverse(bytes);
		return convertByteList(bytes);
	}

	
	private static byte[] decryptLastByte(byte[] secondLast, byte[] last){
		
		//initialize new random array with last byte 0
		randomBlock = getRandomArray(16);
		randomBlock[15] = 0;
		
		//increment the value of the last byte of r
		byte[] test = concatenate(randomBlock,last);
		while(!checkOracle(test)){
			test[15] += 1;
			System.out.println("decrpytByte " + test[15]);
		}		
				
		//oracle returned only yes when swapping
		boolean oracleValue = true;
		
		int l = 0;
		for(int k=0; k<15; k++){
			//replace the byte with another one
			if(test[k] != 127)
				test[k] = 127;
			else
				test[k] = 126;
			
			//check the new sequence
			if(!checkOracle(test)){
				oracleValue = false;
				l = k;
				break;
			}
				
		}
		
		for(int i = 0;i<=15;i++){
			randomBlock[i] = test[i];
		}
		
		byte key;
		if(oracleValue)
			key = (byte) (test[15] ^ 1);
		else
			key = (byte) (test[15] ^ (17 - l - 1));

		return new byte[]{(byte) (key ^ secondLast[15]), key};
	}
	

	
	private static boolean checkOracle(byte[] bytes){
		System.out.println("I am being called to check this: " + Arrays.toString(bytes));
		int i = -1;
		try {
			File test = new File("test");
			FileOutputStream w = new FileOutputStream(test,false);
			w.write(bytes);
			w.close();
			
			Process p = Runtime.getRuntime().exec(oracle+ " test");
			InputStream in = p.getInputStream();
			Scanner sc = new Scanner(in);
			if(sc.hasNextInt()){
				i = sc.nextInt();
				System.out.println("The received output from the orcale is "+i);
			}
			if(sc.hasNext())
				System.out.println(sc.nextLine());
			
			sc.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(i == -1)
			System.out.println("Received weird output or no output from the oracle");
		if(i == 1)
			return true;
		if(i == 0)
			return false;
		return false;
	}
	
	private static byte[] convertByteList(ArrayList<Byte> keys) {
		byte[] temp = new byte[keys.size()];
		for(int i = 0; i< keys.size();i++){
			temp[i] = keys.get(i);
		}
		return temp;
	}
	
	public static byte[] getRandomArray(int size){
		byte[] local = new byte[size];
		new Random().nextBytes(local);
		return local;
	}
	
	public static byte[] concatenate(byte[] r,byte[] y){
		byte[] test = new byte[r.length+y.length];
		System.arraycopy(r, 0, test, 0, r.length);
		System.arraycopy(y, 0, test, r.length, y.length);
		return test;
	}

}
