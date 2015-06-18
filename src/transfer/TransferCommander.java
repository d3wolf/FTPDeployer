package transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class TransferCommander {

	private String configFolderPath;

	private List<File> propertyFiles = new ArrayList<File>();

	public TransferCommander(String folder) {
		String projectPath = System.getProperty("user.dir");
		projectPath = projectPath.replace("\\", "/");
		configFolderPath = projectPath + folder;
		// System.out.println("config file folder: " + configFolderPath);

		getAllPropertyFiles();
		// loadProperties();
	}

	private void getAllPropertyFiles() {
		File propertyFolder = new File(configFolderPath);
		File[] files = propertyFolder.listFiles();

		for (File file : files) {
			if (file.getName().endsWith(".properties")) {
				propertyFiles.add(file);
			}
		}
	}

	private Properties loadProperties(File file) {
		Properties property = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			property.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return property;
	}

	private void printProperty(File file, int index) {
		Properties property = loadProperties(file);
		StringBuffer sb = new StringBuffer();
		sb.append(index + "  |" + file.getName() + "\n");
		sb.append("^  |from: " + property.getProperty("TRANSFER_PATH") + "\n");
		sb.append("   |to: " + property.getProperty("FTP_HOST") + "\n");
		sb.append("*****************************************************");
		System.out.println(sb);
	}
	
	public static void main(String[] args) throws Exception {
		TransferCommander command = new TransferCommander("/configs");

		for (int i = 0; i < command.propertyFiles.size(); i++) {
			command.printProperty(command.propertyFiles.get(i), i);
		}

		boolean validIndex = false;
		do {
			Scanner sc2 = new Scanner(System.in);
			System.out.println("input an index above for transfer:");
			String indexStr = sc2.nextLine();

			try {
				int index = Integer.parseInt(indexStr);
				if (index < command.propertyFiles.size() && index >= 0) {
					validIndex = true;
					
					File file = command.propertyFiles.get(index);
					Properties property = command.loadProperties(file);
					FTPTransfer.transferByProperty(property);
					
				} else if (index == -1) {
					validIndex = true;
					break;
				}else{
					System.out.println("index out of array bound.");
				}
			} catch (NumberFormatException e) {
				System.out.println("invalid index format.");
			}
		} while (!validIndex);
	}

}
