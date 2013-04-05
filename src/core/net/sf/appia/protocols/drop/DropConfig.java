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
 package net.sf.appia.protocols.drop;

//////////////////////////////////////////////////////////////////////
//                                                                  //
// Appia: protocol development and composition framework            //
//                                                                  //
// Version: 1.0/J                                                   //
//                                                                  //
// Copyright, 2000, Universidade de Lisboa                          //
// All rights reserved                                              //
// See license.txt for further information                          //
//                                                                  //
// Interface: DropConfig: Contains some final contants              //
//       that are needed run some code just some times.             //
//       (e.g. for debug)                                           //
//                                                                  //
// Author: Nuno Carvalho, Hugo Miranda - 11 Jul 2001                //
//                                                                  //
//////////////////////////////////////////////////////////////////////

/**
 * Interface DropConfig <br>
 * To use this, just change the value of the specific variable and recompile
 * this interface and the code that uses the variable.
 */

public interface DropConfig {
    public static final boolean debugOn = false;
}
