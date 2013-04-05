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

package net.sf.appia.protocols.group.utils;

import java.text.ParseException;

import net.sf.appia.protocols.group.Endpt;

public class ParseUtils {

    /**
     * Generate an Endpt[] from a string of the form "endpt1[,endpt2 ...]".
     * 
     * @param s the string to parse
     * @return an Endpt array
     * @throws ParseException
     */
    public static Endpt[] parseEndptArray(String s) 
    throws ParseException {
      final char separator=',';
      int isep=-1;
      int previsep;
      int j;

      int count=1;
      while (isep < s.length()) {
        isep=s.indexOf(separator,isep+1);
        if (isep < 0)
          break;
        count++;
      }
      final Endpt[] result=new Endpt[count];
   
      j=0;
      isep=-1;
      while (isep < s.length()) {
        previsep=isep;
        isep=s.indexOf(separator,previsep+1);
        if (isep < 0)
          isep=s.length();
        
        if (isep > previsep+1) {
            result[j++]=new Endpt(s.substring(previsep+1,isep));
        } else {
          throw new ParseException("Missing element in array.",previsep+1);
        }
      }
      
      return result;
    }

}
