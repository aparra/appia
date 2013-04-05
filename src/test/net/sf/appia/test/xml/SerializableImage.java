/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */
 /*
 * Created on 2/Abr/2004
 */
package net.sf.appia.test.xml;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

/**
 * Allows an image to be serialized.
 * 
 * @author Jose Mocito
 */

public class SerializableImage implements Serializable {
	
	private static final long serialVersionUID = -3944556233101481577L;
	private byte[] data;
	
	public SerializableImage() {
		super();
	}
	
	public SerializableImage(byte[] data) {
		this.data = data;
	}
	
	public SerializableImage(BufferedImage image) {
		File file = new File("img.png");
		try {
			ImageIO.write(image,"png",file);
			data = new byte[(int)file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(data);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		data = (byte[])in.readObject();
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(data);
		out.flush();        
	}
	
	public BufferedImage getImage() {
		File file = new File("img.png");
		try {
			FileOutputStream fout = new FileOutputStream(file);
			fout.write(data);
			fout.close();
			BufferedImage img = ImageIO.read(file);
			return img;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}    
	
	public byte[] getData() {
		return data;
	}
}
