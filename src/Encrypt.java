import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Encrypt {

	public static void main(String[] args) {
		Path location = Paths.get(args[0]);
		try {
			byte[] data = Files.readAllBytes(location);
			if(data.length % 16 != 0){
				System.out.println("the received that is not multiple of 16");
				return;
			}
			byte[] enc = encryptInput(data);
			System.out.print("\033[H\033[2J");
			System.out.flush();
			System.out.println("\n\nThe answer I could derive is(with UTF-8 encoding):\n" + new String(enc,"UTF-8"));
			System.out.println("\n\nThe answer I could derive is(byte representation):\n" + Arrays.toString(enc));
			java.nio.file.Files.write(location, enc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private static byte[] encryptInput(byte[] data){
		int numBlocks = data.length/16;
		ArrayList<byte[]> answer = new ArrayList<>();
		for(int i=1; i <= numBlocks;i++){
			if(i==1){
				byte[] iv = Decrypt.getRandomArray(16);
				byte[] block = new byte[16];
				for(int m =0;m<16;m++){
					block[m] = data[(i-1)*16 +m];
				}
				answer.add(Decrypt.decryptInput(Decrypt.concatenate(iv,block)));
			}
		}
		
		byte[] finalAnswer = new byte[answer.size()*answer.get(0).length];
		int index = 0;
		for(byte[] a :  answer){
			for(int i = 0; i<a.length;i++){
				finalAnswer[index] = a[i];
				index++;
			}
		}
		
		return finalAnswer;
	}

}
