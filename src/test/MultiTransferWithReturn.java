package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.net.ftp.FTPClient;

import transfer.FTPTransfer;

public class MultiTransferWithReturn implements Callable<Boolean> {

	private File file;
	FTPClient ftp;
	
	private String workingDir;
	private String host;
	private int port;
	private String userName;
	private String password;

	public MultiTransferWithReturn(String workingDir,String host,int port,String userName,String password, File file){
		this.file = file;
		this.workingDir = workingDir;
		this.host = host;
		this.port = port;
		this.userName = userName;
		this.password = password;
	}

	@Override
	public Boolean call() throws Exception {
		Boolean result = false;
		try {
			ftp = FTPTransfer.getFtp(workingDir, host, port, userName, password);
			System.out.println(workingDir+"-----------------" + ftp.printWorkingDirectory());
			
			if(file.isDirectory()){
				ftp.makeDirectory(file.getName());
				ftp.changeWorkingDirectory(file.getName());
			}else{
				FileInputStream input = new FileInputStream(file);
				ftp.storeFile(FTPTransfer.encodedfileNameToFtp(file.getName()), input);
				input.close();
				FTPTransfer.logger.info(Thread.currentThread().getName() + "->upload success:" + ftp.printWorkingDirectory() + "/" + file.getName());
			}
			
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(ftp!=null){
				try {
					ftp.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

}
