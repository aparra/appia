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
 package net.sf.appia.protocols.group;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.MsgBuffer;



/**
 * Title:
 * Description:
 * @author Alexandre Pinto
 * @version 1.0
 */

public class ArrayOptimized {

  private static byte[]
    BITS={(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};


    public static void pushArrayBoolean(boolean[] booleans, Message message)
    {
        MsgBuffer buf=new MsgBuffer();
        buf.len=booleans.length/8;
        if ((booleans.length%8)>0)
            buf.len++;
        message.push(buf);
        int k=0,j=0;
        for (int i=0;i<booleans.length;i++)
        {
            if (j==0)
            {
                buf.data[buf.off+k]=0;
            }
            if (booleans[i])
                buf.data[buf.off+k]|=BITS[j];
            if (j==7)
            {
                j=0;
                k++;
            }
            else
                j++;
        }
        message.pushInt(booleans.length);
    }



    public static boolean[] popArrayBoolean( Message message)
    {
        boolean[] booleans=new boolean[message.popInt()];
        MsgBuffer buf=new MsgBuffer();
        buf.len=booleans.length/8;
        if ((booleans.length%8)>0)
            buf.len++;
        message.pop(buf);
        int k=0,j=0;
        for (int i=0;i<booleans.length;i++)
        {
            if ((buf.data[buf.off+k]&BITS[j])!=0)
                booleans[i]=true;
            else
                booleans[i]=false;
            if (j==7)
            {
                j=0;
                k++;
            }
            else
                j++;
        }
        return booleans;
    }


    public static boolean[] peekArrayBoolean( Message message)
    {
        boolean[] booleans=ArrayOptimized.popArrayBoolean(message);
        ArrayOptimized.pushArrayBoolean(booleans,message);
        return booleans;
    }




    public static void pushArrayViewID(ViewID[] viewids, Message message)
    {
        for(int i=viewids.length-1; i>-1; i--)
        {
            ViewID.push(viewids[i],message);
        }
        message.pushInt(viewids.length);
    }

    public static ViewID[] popArrayViewID(Message message)
    {
        int size=message.popInt();
        ViewID[] viewids=new ViewID[size];
        for(int i=0;i<viewids.length; i++)
        {
            viewids[i]=ViewID.pop(message);
        }
        return viewids;
    }

    public static ViewID[] peekArrayViewID(Message message)
    {
        ViewID[] viewids=ArrayOptimized.popArrayViewID(message);
        ArrayOptimized.pushArrayViewID(viewids,message);
        return viewids;
    }

    public static void pushArrayInetWithPort(SocketAddress[] inetp, Message message)
    {
        for(int i=inetp.length-1; i>-1; i--){
            message.pushObject(inetp[i]);
        }
        message.pushInt(inetp.length);
    }

    public static InetSocketAddress[] popArrayInetWithPort(Message message)
    {
        int size=message.popInt();
        InetSocketAddress[] inetps=new InetSocketAddress[size];
        for(int i=0;i<inetps.length; i++)
        {
            inetps[i]=(InetSocketAddress) message.popObject();
        }
        return inetps;
    }

    public static InetSocketAddress[] peekArrayInetWithPort(Message message)
    {
        InetSocketAddress[] inetps=ArrayOptimized.popArrayInetWithPort(message);
        ArrayOptimized.pushArrayInetWithPort(inetps,message);
        return inetps;
    }

    public static void pushArrayEndpt(Endpt[] endpt, Message message)
    {
        for(int i=endpt.length-1; i>-1; i--)
        {
            Endpt.push(endpt[i],message);
        }
        message.pushInt(endpt.length);
    }

    public static Endpt[] popArrayEndpt(Message message)
    {
        int size=message.popInt();
        Endpt[] endpts=new Endpt[size];
        for(int i=0;i<endpts.length; i++)
        {
            endpts[i]=Endpt.pop(message);
        }
        return endpts;
    }

    public static Endpt[] peekArrayEndpt(Message message)
    {
        Endpt[] endpts=ArrayOptimized.popArrayEndpt(message);
        ArrayOptimized.pushArrayEndpt(endpts,message);
        return endpts;
    }

    public static void pushArrayInt(int[] ints,Message message)
    {
        for (int i=ints.length-1;i>-1;i--)
        {
            message.pushInt(ints[i]);
        }
        message.pushInt(ints.length);
    }

    public static int[] popArrayInt(Message message)
    {
        int[] ints=new int[message.popInt()];
        for (int i=0;i<ints.length;i++)
        {
            ints[i]=message.popInt();
        }
        return ints;
    }

    public static int[] peekArrayInt(Message message)
    {
        int[] ints=ArrayOptimized.popArrayInt(message);
        ArrayOptimized.pushArrayInt(ints,message);
        return ints;
    }

    public static void pushArrayLong(long[] longs,Message message)
    {
        for (int i=longs.length-1;i>-1;i--)
        {
            message.pushLong(longs[i]);
        }
        message.pushInt(longs.length);
    }

    public static long[] popArrayLong(Message message)
    {
        long[] longs=new long[message.popInt()];
        for (int i=0;i<longs.length;i++)
        {
            longs[i]=message.popLong();
        }
        return longs;
    }

    public static long[] peekArrayLong(Message message)
    {
        long[] longs=ArrayOptimized.popArrayLong(message);
        ArrayOptimized.pushArrayLong(longs,message);
        return longs;
    }

}