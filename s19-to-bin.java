
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.HexFormat;

	public static void main(String[] args) throws IOException {
		
		if (args.length< 2) {
			System.out.println("s19 <[s19 file] [bin output] | [bin file] [s19 output]>");
			System.exit(1);
		}
		
		File inFile = new File(args[0]);
		File outFile = new File(args[1]);
		
		if (inFile.getName().endsWith(".s19")) {
			s19ToBin(inFile, outFile);
		} else if (inFile.getName().endsWith(".bin")) {
			binToS19(inFile, outFile, 4, 0x08000000, 32);
		}
		
	}
	
	public static void binToS19(File binFile, File s19File, int addrLen, long mapAddrZero, int recordDataLen) throws IOException {
		
		if (addrLen < 2 || addrLen > 4) {
			System.err.println("Invalid address size!");
			System.exit(-1);
		}
		
		int recordType = addrLen == 2 ? 1 : addrLen == 3 ? 2 : 3;
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(s19File)));
		InputStream input = new FileInputStream(binFile);
		
		long datapos = mapAddrZero;
		byte[] data = new byte[recordDataLen];
		int dlen = 0;
		while ((dlen = input.read(data)) > 0) {
			
			StringBuffer recordEntry = new StringBuffer();
			
			recordEntry.append("S");
			recordEntry.append(HexFormat.of().toHexDigits(recordType, 1));
			recordEntry.append(HexFormat.of().toHexDigits(dlen + 1 + addrLen, 2));
			recordEntry.append(HexFormat.of().toHexDigits(datapos, 8));
			recordEntry.append(HexFormat.of().formatHex(data, 0, dlen));

			byte checksum = (byte) (
					(byte) ((datapos >> 0) & 0xFF) +
					(byte) ((datapos >> 8) & 0xFF) +
					(byte) ((datapos >> 16) & 0xFF) +
					(byte) ((datapos >> 24) & 0xFF) +
					(byte) (dlen + 1 + addrLen));
			for (int i = 0; i < data.length; i++)
				checksum += data[i] & 0xFF;
			checksum = (byte) (~checksum & 0xFF);
			
			recordEntry.append(HexFormat.of().toHexDigits(checksum, 2));
			
			System.out.println("S File : " + recordEntry);
			
			writer.write(recordEntry.toString().toUpperCase());
			writer.write('\n');
			
			datapos += dlen;
			
		}
		
		writer.close();
		input.close();
		
		System.exit(0);
		
	}
	
	public static void s19ToBin(File s19File, File binFile) throws IOException {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(s19File)));
		OutputStream output = new FileOutputStream(binFile);
		
		String line;
		long datpos = 0;
		while ((line = reader.readLine()) != null) {
			if (line.charAt(0) != 'S') continue;
			if (line.length() < 4) {
				System.err.println("incomplete line: " + line);
				System.exit(-1);
			}
			byte type = Byte.parseByte(line.substring(1, 2), 16);
			int len = Integer.parseInt(line.substring(2, 4), 16);
			if (line.length() < 4 + len * 2) {
				System.err.println("incomplete line: " + line);
				System.exit(-1);
			}
			System.out.println(String.format("S File : S %d len %d content %s", type, len, line.substring(4)));
			
			switch (type) {
			case 0: continue;
			case 1: datpos = writeData(output, datpos, 2, line.substring(4)); break;
			case 2: datpos = writeData(output, datpos, 3, line.substring(4)); break;
			case 3: datpos = writeData(output, datpos, 4, line.substring(4)); break;
			case 5: continue;
			case 6: continue;
			case 7: continue;
			case 8: continue;
			case 9: continue;
			default:
				System.err.println("Invalid type!");
				System.exit(-3);
			}
			
		}
		
		reader.close();
		output.close();
		
		System.exit(0);
		
	}
	
	public static long writeData(OutputStream output, long pos, int addressBytes, String hex) throws IOException {
		
		long address = Long.parseLong(hex.substring(0, addressBytes * 2), 16);
		byte[] data = HexFormat.of().parseHex(hex.substring(addressBytes * 2, hex.length() - 2));
		
		if (address > pos) {
			if (pos == 0) {
				System.out.println("First byte written: " + hex.substring(0, addressBytes * 2));
			} else {	
				System.out.println("Skipped bytes: 0x" + (address - pos));
				output.write(new byte[(int) (address - pos)]);
			}
			pos = address;
		}
		
		output.write(data);
		pos += data.length;
		return pos;
		
	}
