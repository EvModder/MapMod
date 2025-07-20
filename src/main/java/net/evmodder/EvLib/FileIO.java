package net.evmodder.EvLib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public final class FileIO{
	public static final String DIR;//= FabricLoader.getInstance().getConfigDir().toString()+"/";
	static{
		String tempDir = "./";
		try{
			Object fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getInstance").invoke(null);
			tempDir = fabricLoader.getClass().getMethod("getConfigDir").invoke(fabricLoader).toString()+"/"+
						Class.forName("net.evmodder.KeyBound.Main").getField("MOD_ID").get(null)+"/";
		}
		catch(IllegalArgumentException | NoSuchFieldException e){e.printStackTrace();}
		catch(ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e){}
		DIR = tempDir;
	}

	public static final String loadFile(String filename, String defaultValue){
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader(DIR+filename));}
		catch(FileNotFoundException e){
			if(defaultValue == null) return null;

			//Create Directory
			final File dir = new File(DIR);
			if(!dir.exists())dir.mkdir();

			//Create the file
			final File conf = new File(DIR+filename);
			try{
				conf.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
				writer.write(defaultValue); writer.close();
				reader = new BufferedReader(new FileReader(DIR+filename));
			}
			catch(IOException e1){e1.printStackTrace();}
		}
		final StringBuilder file = new StringBuilder();
		if(reader != null){
			try{
				String line = reader.readLine();
				while(line != null){
					line = line.trim().replace("//", "#");
					int cut = line.indexOf('#');
					if(cut == -1) file.append('\n').append(line);
					else if(cut > 0) file.append('\n').append(line.substring(0, cut).trim());
					line = reader.readLine();
				}
				reader.close();
			}catch(IOException e){}
		}
		return file.length() == 0 ? "" : file.substring(1);
	}
	public static final String loadFile(String filename, InputStream defaultValue){
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader(DIR+filename));}
		catch(FileNotFoundException e){
			if(defaultValue == null) return null;

			//Create Directory
			final File dir = new File(DIR);
			if(!dir.exists())dir.mkdir();

			//Create the file
			final File conf = new File(DIR+filename);
			try{
				conf.createNewFile();
				reader = new BufferedReader(new InputStreamReader(defaultValue));

				String line = reader.readLine();
				StringBuilder builder = new StringBuilder(line);
				while((line = reader.readLine()) != null) builder.append('\n').append(line);
				reader.close();

				BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
				writer.write(builder.toString()); writer.close();
				reader = new BufferedReader(new FileReader(DIR+filename));
			}
			catch(IOException e1){e1.printStackTrace();}
		}
		final StringBuilder file = new StringBuilder();
		if(reader != null){
			try{
				String line = reader.readLine();
				while(line != null){
					line = line.trim().replace("//", "#");
					int cut = line.indexOf('#');
					if(cut == -1) file.append('\n').append(line);
					else if(cut > 0) file.append('\n').append(line.substring(0, cut).trim());
					line = reader.readLine();
				}
				reader.close();
			}catch(IOException e){}
		}
		return file.length() == 0 ? "" : file.substring(1);
	}
	public static final byte[] loadFileBytes(String filename){
		try{
			final FileInputStream fis = new FileInputStream(FileIO.DIR+filename);
			final byte[] data = fis.readAllBytes();
			fis.close();
			return data;
		}
		catch(FileNotFoundException e){return null;}
		catch(IOException e){e.printStackTrace(); return null;}
	}
	public static final boolean saveFileBytes(String filename, byte[] data){
		File file = new File(FileIO.DIR+filename);
		FileOutputStream fos;
		try{
			try{fos = new FileOutputStream(file);}
			catch(FileNotFoundException e){
				file.createNewFile();
				fos = new FileOutputStream(file);
			}
			fos.write(data);
			fos.close();
		}
		catch(IOException e){e.printStackTrace(); return false;}
		return true;
	}

	public static boolean saveFile(String filename, String content, boolean append){
		if(content == null || content.isEmpty()) return new File(DIR+filename).delete();
		try{
			final BufferedWriter writer = new BufferedWriter(new FileWriter(DIR+filename, append));
			writer.write(content); writer.close();
			return true;
		}
		catch(IOException e){return false;}
	}
	public static boolean saveFile(String filename, String content){
		return saveFile(filename, content, /*append=*/false);
	}
}