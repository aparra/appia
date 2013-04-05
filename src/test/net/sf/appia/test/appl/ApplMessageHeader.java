package net.sf.appia.test.appl;

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.SerializableObject;

public class ApplMessageHeader implements SerializableObject {

	public String message;
	public int number;
	
	public ApplMessageHeader() {
		super();
	}

	public ApplMessageHeader(Message m) {
		super();
		popMySelf(m);
	}

	public ApplMessageHeader(String m, int n) {
		super();
		message = m;
		number = n;
	}

	public void pushMySelf(Message m) {
		m.pushString(message);
		m.pushInt(number);
	}

	public void popMySelf(Message m) {
		number = m.popInt();
		message = m.popString();
	}

	public void peekMySelf(Message m) {
		number = m.peekInt();
		message = m.peekString();
	}

}
