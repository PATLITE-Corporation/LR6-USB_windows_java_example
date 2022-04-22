/**
 * 
 */
package jp.co.patlite.lr6_usb.sample;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.usb4java.Context;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;

/**
 * @author
 *
 */
public class Main {

	public static void main(String[] args) {
		Control ctr = new Control();
		
		// Connect to LR-USB
		int ret = ctr.usb_open();
		if(ret < 0 ) {
 			System.out.println("device not found");
			return;
		}
		
		try {
			// Argument check
			String commandId = " ";
            if (args.length > 0) {
                commandId = args[0];
            }

            // Command judgment
            switch (commandId)
            {
                case "1":
                {
                  	// LED unit control
                    if (args.length >= 3)
                       	ctr.set_light((byte)Integer.parseInt(args[1]), (byte)Integer.parseInt(args[2]));
                    break;
                }

                case "2":
                {
                	// Multiple LED unit control
                    if (args.length >= 6)
                    	ctr.set_tower((byte)Integer.parseInt(args[1]), (byte)Integer.parseInt(args[2]), (byte)Integer.parseInt(args[3]), (byte)Integer.parseInt(args[4]), (byte)Integer.parseInt(args[5]));
                    break;
                }

                case "3":
                {
                	// Buzzer control with buzzer pattern
                    if (args.length >= 3)
                    	ctr.set_buz((byte)Integer.parseInt(args[1]), (byte)Integer.parseInt(args[2]));
                    break;
                }

                case "4":
                {
                	// Control the buzzer with the buzzer pattern and scale
                    if (args.length >= 5)
                    	ctr.set_buz_ex((byte)Integer.parseInt(args[1]), (byte)Integer.parseInt(args[2]), (byte)Integer.parseInt(args[3]), (byte)Integer.parseInt(args[4]));
                    break;
                }

                case "5":
                {
                	// reset
                   	ctr.reset();
                    break;
                }
            }
                    
		} finally {
			// End processing
			ctr.usb_close();
		}
	}

}

class Control {

	// Vendor ID
	private static int VENDOR_ID = 0x191A;
	// Device ID
	private static int DEVICE_ID = 0x8003;
	// Command version
	private static byte COMMAND_VERSION = 0x00;
	// Command ID
	private static byte COMMAND_ID = 0x00;
	// Endpoint address for sending to host -> USB controlled stacked signal light
	private static byte	ENDPOINT_ADDRESS = 0x01;
	// Time-out time when sending a command
	private static int	SEND_TIMEOUT = 1000;
	// Protocol data area size
	private static byte	SEND_BUFFER_SIZE = 8;
	// openings
	private static byte	BLANK = 8;
	
	// LED unit color
	private static byte LED_COLOR_RED = 0;			// Red
	private static byte LED_COLOR_YELLOW = 1;		// yellow
	private static byte LED_COLOR_GREEN = 2;		// green
	private static byte LED_COLOR_BLUE = 3;			// Blue
	private static byte LED_COLOR_WHITE = 4;		// White

	// LED pattern
	private static byte	LED_OFF = 0x00;				// Off
	private static byte LED_KEEP = 0x0F;			// Keep the current settings

	// Buzzer pattern
	private static byte BUZZER_OFF = 0x00;			// Stop
	private static byte	BUZZER_KEEP = 0x0F;			// Keep the current settings

	// Buzzer scale
	private static byte BUZZER_PITCH_OFF = 0x00;	// Stop
	private static byte BUZZER_PITCH_DFLT_A = 0x0E;	// Default value for sound A: D7
	private static byte BUZZER_PITCH_DFLT_B = 0x0F;	// Default value for sound B: (stop)

	
	// Internal variables
	Context context;
	DeviceHandle devHandle;
	
	/**
	 * constructor
	 * 
	 */
	Control() {
		context = null;
		devHandle = null;
	}
	
	/**
	 * Connect to server
	 * 
	 * @return successÅF0, FailureÅFNon-zero
	 */
	public int usb_open() {
 		int ret = 0;
		// Initialization process
 		context = new Context();
 		int initret = LibUsb.init(context);
 		if(initret != LibUsb.SUCCESS) {
 			return -1;
 		}
 		
 		// Device open
 		devHandle = LibUsb.openDeviceWithVidPid(context, (short)VENDOR_ID, (short)DEVICE_ID);
 		if(devHandle == null) {
 			return -1;
 		}
 		
 		// Interface acquisition
 		int IntRet = LibUsb.claimInterface(devHandle, 0);
 		if(IntRet != 0) {
 			return -2;
 		}

		return ret;
	}

	/**
	 * Close socket
	 */
	public void usb_close() {
		// End processing
		LibUsb.close(devHandle);
		LibUsb.exit(context);
	}

	/**
	 * Send command
	 * 
	 * @param sendData Transmission data
	 * @return Send result (number of bytes sent, negative value is error)
	 */
	private int send_command(final byte[] sendData) {
		int ret = 0;
		
		// Convert data
		ByteBuffer setData = ByteBuffer.allocateDirect(SEND_BUFFER_SIZE);
		setData.put(sendData);
		
		// Check the handle
		if(devHandle == null) {
			return 0;
		}
		
		// data transfer
		IntBuffer sendLength = IntBuffer.allocate(SEND_BUFFER_SIZE);
		int TranRet = LibUsb.interruptTransfer(devHandle, ENDPOINT_ADDRESS, setData, sendLength, (long)SEND_TIMEOUT);
		if(TranRet == 0) {
			ret = sendLength.get();
		}else{
			ret = -1;
		}
		
		return ret;
	}

	/**
	 * LED unit control<br>
	 * Specify the LED color and LED pattern to turn on and turn on the pattern.
	 * 
	 * @param color LED color to control (red: 0, yellow: 1, green: 2, blue: 3, white: 4)
	 * @param state LED pattern (off: 0x00, on: 0x01, LED pattern 1: 0x02, LED pattern 2: 0x03, LED pattern 3: 0x04, LED pattern 4: 0x05, maintain current settings: 0x06 to 0x0F)
	 * @return Success: 0, Failure: Other than 0
	 */

	public int set_light(final byte color, final byte state) {
		byte[] sendData = new byte[Control.SEND_BUFFER_SIZE];

		// Command version
		sendData[0] = Control.COMMAND_VERSION;

		// Command ID
		sendData[1] = Control.COMMAND_ID;

		// Buzzer control
		sendData[2] = Control.BUZZER_KEEP;

		// Buzzer scale
		sendData[3] = Control.BUZZER_PITCH_OFF;
		
		// LED (red / yellow)
		sendData[4] = (byte) ((Control.LED_KEEP << 4) | Control.LED_KEEP);
		if(color == Control.LED_COLOR_RED) {
			sendData[4] = (byte) ((state << 4) | Control.LED_KEEP);
		}
		if(color == Control.LED_COLOR_YELLOW) {
			sendData[4] = (byte) ((Control.LED_KEEP << 4) | state);
		}
		
		// LED (green / blue)
		sendData[5] = (byte) ((Control.LED_KEEP << 4) | Control.LED_KEEP);
		if(color == Control.LED_COLOR_GREEN) {
			sendData[5] = (byte) ((state << 4) | Control.LED_KEEP);
		}
		if(color == Control.LED_COLOR_BLUE) {
			sendData[5] = (byte) ((Control.LED_KEEP << 4) | state);
		}
		
		// LED (white)
		sendData[6] = (byte) ((Control.LED_KEEP << 4) | Control.LED_OFF);
		if(color == Control.LED_COLOR_WHITE) {
			sendData[6] = (byte) ((state << 4) | Control.LED_OFF);
		}
		
		// openings
		sendData[7] = Control.BLANK;
	

		// Send command
		int ret = this.send_command(sendData);
		if (ret <= 0) {
			System.err.println("failed to send data");
		}

		return ret;
	}

	/**
	 * Multiple LED unit control<br>
	 * Specify multiple LED colors and LED patterns to light the pattern.
	 * 
	 * @param red Red LED pattern (off: 0x00, on: 0x01, LED pattern 1: 0x02, LED pattern 2: 0x03, LED pattern 3: 0x04, LED pattern 4: 0x05, maintain current settings: 0x06 to 0x0F)
	 * @param yellow Yellow LED pattern (off: 0x00, on: 0x01, LED pattern 1: 0x02, LED pattern 2: 0x03, LED pattern 3: 0x04, LED pattern 4: 0x05, maintain the current settings: 0x06 to 0x0F)
	 * @param green Green LED pattern (off: 0x00, on: 0x01, LED pattern 1: 0x02, LED pattern 2: 0x03, LED pattern 3: 0x04, LED pattern 4: 0x05, maintain current settings: 0x06-0x0F)
	 * @param blue Blue LED pattern (off: 0x00, on: 0x01, LED pattern 1: 0x02, LED pattern 2: 0x03, LED pattern 3: 0x04, LED pattern 4: 0x05, maintain current settings: 0x06 to 0x0F)
	 * @param white White white pattern (off: 0x00, on: 0x01, LED pattern 1: 0x02, LED pattern 2: 0x03, LED pattern 3: 0x04, LED pattern 4: 0x05, maintain the current settings: 0x06 to 0x0F)
	 * @return Success: 0, Failure: Other than 0
	 */

	public int set_tower(final byte red, final byte yellow , final byte green, final byte blue, final byte white ) {
		byte[] sendData = new byte[Control.SEND_BUFFER_SIZE];

		// Command version
		sendData[0] = Control.COMMAND_VERSION;

		// Command ID
		sendData[1] = Control.COMMAND_ID;

		// Buzzer control
		sendData[2] = Control.BUZZER_KEEP;

		// Buzzer scale
		sendData[3] = Control.BUZZER_PITCH_OFF;
		
		// LED (red / yellow)
		sendData[4] = (byte) ((red << 4) | yellow);
		
		// LED (green / blue)
		sendData[5] = (byte) ((green << 4) | blue);
		
		// LED (white)
		sendData[6] = (byte) ((white << 4) | Control.LED_OFF);
		
		// openings
		sendData[7] = Control.BLANK;
	

		// Send command
		int ret = this.send_command(sendData);
		if (ret <= 0) {
			System.err.println("failed to send data");
		}

		return ret;
	}

	/**
	 * Buzzer control with buzzer pattern<br>
	 * Specify the buzzer pattern and the buzzer sounds.
	 * The LED unit will maintain its current state and the scale will operate with default values.
	 * 
	 * @param buz_state Buzzer pattern
	 * @param limit Continuous operation: 0, number of operations: 1 to 15
	 * @return Success: 0, Failure: Other than 0
	 */

	public int set_buz(final byte buz_state, final byte limit ) {
		byte[] sendData = new byte[Control.SEND_BUFFER_SIZE];

		// Command version
		sendData[0] = Control.COMMAND_VERSION;

		// Command ID
		sendData[1] = Control.COMMAND_ID;

		// Buzzer control
		sendData[2] = (byte) ((limit << 4) | buz_state);

		// Buzzer scale
		sendData[3] = (byte) ((Control.BUZZER_PITCH_DFLT_A << 4) | Control.BUZZER_PITCH_DFLT_B);
		
		// LED (red / yellow)
		sendData[4] = (byte) ((Control.LED_KEEP << 4) | Control.LED_KEEP);
		
		// LED (green / blue)
		sendData[5] = (byte) ((Control.LED_KEEP << 4) | Control.LED_KEEP);
		
		// LED (white)
		sendData[6] = (byte) ((Control.LED_KEEP << 4) | Control.LED_OFF);
		
		// openings
		sendData[7] = Control.BLANK;
	

		// Send command
		int ret = this.send_command(sendData);
		if (ret <= 0) {
			System.err.println("failed to send data");
		}

		return ret;
	}

	/**
	 * Control the buzzer with the buzzer pattern and scale<br>
	 * The buzzer sounds by specifying the scale and pattern of the buzzer.
	 * The LED unit maintains its current state.
	 * 
	 * @param buz_state Buzzer pattern
	 * @param limit Continuous operation: 0, number of operations: 1 to 15
	 * @param pitch1 Sound A buzzer scale (stop: 0x00, A6: 0x01, B ÅÛ 6: 0x02, B6: 0x03, C7: 0x04, D ÅÛ 7: 0x05, D7: 0x06, E ÅÛ 7: 0x07, E7: 0x08, F7: 0x09, G ÅÛ 7: 0x0A, G7: 0x0B, A ÅÛ 7: 0x0C, A7: 0x0D, default value of sound A: D7: 0x0E, default value of sound B: (stop): 0x0F) 
	 * @param pitch2 Sound B buzzer scale (stop: 0x00, A6: 0x01, B ÅÛ 6: 0x02, B6: 0x03, C7: 0x04, D ÅÛ 7: 0x05, D7: 0x06, E ÅÛ 7: 0x07, E7: 0x08, F7: 0x09, G ÅÛ 7: 0x0A, G7: 0x0B, A ÅÛ 7: 0x0C, A7: 0x0D, default value of sound A: D7: 0x0E, default value of sound B: (stop): 0x0F) 
	 * @return Success: 0, Failure: Other than 0
	 */

	public int set_buz_ex(final byte buz_state, final byte limit, final byte pitch1, final byte pitch2 ) {
		byte[] sendData = new byte[Control.SEND_BUFFER_SIZE];

		// Command version
		sendData[0] = Control.COMMAND_VERSION;

		// Command ID
		sendData[1] = Control.COMMAND_ID;

		// Buzzer control
		sendData[2] = (byte) ((limit << 4) | buz_state);

		// Buzzer scale
		sendData[3] = (byte) ((pitch1 << 4) | pitch2);
		
		// LED (red / yellow)
		sendData[4] = (byte) ((Control.LED_KEEP << 4) | Control.LED_KEEP);
		
		// LED (green / blue)
		sendData[5] = (byte) ((Control.LED_KEEP << 4) | Control.LED_KEEP);
		
		// LED (white)
		sendData[6] = (byte) ((Control.LED_KEEP << 4) | Control.LED_OFF);
		
		// openings
		sendData[7] = Control.BLANK;
	

		// Send command
		int ret = this.send_command(sendData);
		if (ret <= 0) {
			System.err.println("failed to send data");
		}

		return ret;
	}

	/**
	 * reset<br>
	 * Turns off all LED units and stops the buzzer.
	 * 
	 * @return Success: 1 or more, Failure: 0 or less
	 */

	public int reset() {
		byte[] sendData = new byte[Control.SEND_BUFFER_SIZE];

		// Command version
		sendData[0] = Control.COMMAND_VERSION;

		// Command ID
		sendData[1] = Control.COMMAND_ID;

		// Buzzer control
		sendData[2] = Control.BUZZER_OFF;

		// Buzzer scale
		sendData[3] = Control.BUZZER_PITCH_OFF;
		
		// LED (red / yellow)
		sendData[4] = Control.LED_OFF;
		
		// LED (green / blue)
		sendData[5] = Control.LED_OFF;
		
		// LED (white)
		sendData[6] = Control.LED_OFF;
		
		// openings
		sendData[7] = Control.BLANK;
	

		// Send command
		int ret = this.send_command(sendData);
		if (ret <= 0) {
			System.err.println("failed to send data");
		}

		return ret;
	}

}