/**
 * APPIA implementation of JGCS - Group Communication Service
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
 * Initial developer(s): Nuno Carvalho.
 * 
 *  * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 */
 
package net.sf.appia.jgcs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppiaUtils {

	public AppiaUtils() {
		super();
	}
	
	public static <T> Collection<T> toCollection(T[] array){
		return new MyCollection<T>(array);
	}
	
	//FIXME: do this without the suppresswarnings tag
	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(List<T> list){
		return (T[]) list.toArray();
	}
	
}

class MyCollection<E> extends ArrayList<E> {

	private static final long serialVersionUID = 7773068311595478561L;
	
	protected MyCollection(E[] array){
		for(int i=0; i<array.length; i++)
			this.add(array[i]);
	}
	
}
