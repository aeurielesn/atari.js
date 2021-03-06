// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package pc.controls;

import general.av.video.VideoMonitor;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import parameters.Parameters;
import atari.controls.ConsoleControls;
import atari.controls.ConsoleControlsInput;

public class AWTConsoleControls implements ConsoleControls, KeyListener {
	
	public AWTConsoleControls(VideoMonitor monitor) {
		super();
		videoMonitor = monitor;
		initKeys();
	}

	public void connect(ConsoleControlsInput input) {
		consoleControlsInput = input;
	}

	public void addInputComponents(Component... inputs) {
		for (int i = 0; i < inputs.length; i++) {
			inputs[i].setFocusTraversalKeysEnabled(false);
			inputs[i].addKeyListener(this);
		}
	}

	public void p1ControlsMode(boolean state) {
		p1ControlsMode = state;
		initJoystickKeys();
	}
	
	public void paddleMode(boolean state) {
		paddleMode = state;
		paddle0MovingLeft = paddle0MovingRight = paddle1MovingLeft = paddle1MovingRight = false;
		paddle0Speed = paddle1Speed = 2;
		paddle0Position = paddle1Position = (paddleMode ? 190 : -1);	// -1 = disconnected, won't charge POTs
		// Reset all controls to default state
		for (Control control : playerDigitalControls)
			consoleControlsInput.controlStateChanged(control, false);
		consoleControlsInput.controlStateChanged(Control.PADDLE0_POSITION, paddle0Position);
		consoleControlsInput.controlStateChanged(Control.PADDLE1_POSITION, paddle1Position);
		videoMonitor.showOSD((paddleMode ? "Paddles" : "Joysticks") + " connected");
		if (paddleMode) {
			if (paddlePositionUpdater != null && paddlePositionUpdater.isAlive()) return;	// All set
			paddlePositionUpdater = new PaddlesPositionUpdater();
			paddlePositionUpdater.start();
		} else {
			if (paddlePositionUpdater == null || !paddlePositionUpdater.isAlive()) return;	// All set
			try { paddlePositionUpdater.join(); } catch (InterruptedException e) {}
			paddlePositionUpdater = null;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Do nothing
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		int modifiers = e.getModifiersEx();
		if (checkLocalControlKeyPressed(keyCode, modifiers)) return;
		Control control = controlForEvent(keyCode, modifiers);
		if (control == null) return;
		if (paddleMode)	control = translatePaddleModeButtons(control);
		Boolean state = controlStateMap.get(control);
		if (state == null || !state) {
			controlStateMap.put(control, true);
			consoleControlsInput.controlStateChanged(control, true);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		int modifiers = e.getModifiersEx();
		if (checkLocalControlKeyReleased(keyCode, modifiers)) return;
		Control control = controlForEvent(keyCode, modifiers);
		if (control == null) return;
		if (paddleMode)	control = translatePaddleModeButtons(control);
		Boolean state = controlStateMap.get(control);
		if (state == null || state) {
			controlStateMap.put(control, false);
			consoleControlsInput.controlStateChanged(control, false);
		}
	}

	private boolean checkLocalControlKeyPressed(int keyCode, int modif) {
		if (modif == KeyEvent.ALT_DOWN_MASK)
			switch(keyCode) {
				case KEY_LOCAL_P1_MODE:
					p1ControlsMode(!p1ControlsMode); 
					videoMonitor.showOSD((p1ControlsMode ? "P2" : "P1") + " controls mode");
					return true;
				case KEY_LOCAL_PADDLE_MODE:
					paddleMode(!paddleMode); return true;
			}
		if (paddleMode) {
			Control control = controlForEvent(keyCode, modif);
			if (control == null) return false;
			switch(control) {
				case JOY0_LEFT:
					paddle0MovingLeft = true; return true;
				case JOY0_RIGHT:
					paddle0MovingRight = true; return true;
				case JOY0_UP:
					if (paddle0Speed < 10) paddle0Speed++;
					videoMonitor.showOSD("Paddle 1 speed: " + paddle0Speed);
					return true;
				case JOY0_DOWN:
					if (paddle0Speed > 1) paddle0Speed--;
					videoMonitor.showOSD("Paddle 1 speed: " + paddle0Speed);
					return true;
				case JOY1_LEFT:
					paddle1MovingLeft = true; return true;
				case JOY1_RIGHT:
					paddle1MovingRight = true; return true;
				case JOY1_UP:
					if (paddle1Speed < 10) paddle1Speed++;
					videoMonitor.showOSD("Paddle 2 speed: " + paddle1Speed);
					return true;
				case JOY1_DOWN:
					if (paddle1Speed > 1) paddle1Speed--;
					videoMonitor.showOSD("Paddle 2 speed: " + paddle1Speed);
					return true;
			}			
		}
		return false;
	}

	private boolean checkLocalControlKeyReleased(int keyCode, int modif) {
		if (paddleMode) {
			Control control = controlForEvent(keyCode, modif);
			if (control == null) return false;
			switch(control) {
				case JOY0_LEFT:
					paddle0MovingLeft = false; return true;
				case JOY0_RIGHT:
					paddle0MovingRight = false; return true;
				case JOY1_LEFT:
					paddle1MovingLeft = false; return true;
				case JOY1_RIGHT:
					paddle1MovingRight = false; return true;
			}			
		}
		return false;
	}

	private Control controlForEvent(int keyCode, int modif) {
		switch (modif) {
			case 0:
				Control joy = joysticksCodeMap.get(keyCode);
				if (joy != null) return joy;
				return normalCodeMap.get(keyCode);
			case KeyEvent.CTRL_DOWN_MASK:
				return withCTRLCodeMap.get(keyCode);
			case KeyEvent.ALT_DOWN_MASK:
				return withALTCodeMap.get(keyCode);
		}
		return null;
	}

	private Control translatePaddleModeButtons(Control control) {
		switch (control) {
			case JOY0_BUTTON: return Control.PADDLE0_BUTTON;
			case JOY1_BUTTON: return Control.PADDLE1_BUTTON;
		}
		return control;
	}

	private void paddlesUpdatePosition() {
		if (paddle0MovingRight) {
			if (!paddle0MovingLeft) {
				paddle0Position -= paddle0Speed;
				if (paddle0Position < 0) paddle0Position = 0;
				consoleControlsInput.controlStateChanged(Control.PADDLE0_POSITION, paddle0Position);
			}
		} else if (paddle0MovingLeft) {
			paddle0Position += paddle0Speed;
			if (paddle0Position > 380) paddle0Position = 380;
			consoleControlsInput.controlStateChanged(Control.PADDLE0_POSITION, paddle0Position);
		}
		if (paddle1MovingRight) {
			if (!paddle1MovingLeft) {
				paddle1Position -= paddle1Speed;
				if (paddle1Position < 0) paddle1Position = 0;
				consoleControlsInput.controlStateChanged(Control.PADDLE1_POSITION, paddle1Position);
			}
		} else if (paddle1MovingLeft) {
			paddle1Position += paddle1Speed;
			if (paddle1Position > 380) paddle1Position = 380;
			consoleControlsInput.controlStateChanged(Control.PADDLE1_POSITION, paddle1Position);
		}
	}

	private void initKeys() {
		initJoystickKeys(); 
		
		normalCodeMap.put(KEY_POWER,       Control.POWER); 
		normalCodeMap.put(KEY_BLACK_WHITE, Control.BLACK_WHITE); 
		normalCodeMap.put(KEY_DIFFICULTY0, Control.DIFFICULTY0); 
		normalCodeMap.put(KEY_DIFFICULTY1, Control.DIFFICULTY1); 
		normalCodeMap.put(KEY_SELECT,      Control.SELECT); 
		normalCodeMap.put(KEY_RESET,       Control.RESET); 
		normalCodeMap.put(KEY_FAST_SPEED,  Control.FAST_SPEED); 
		
		withALTCodeMap.put(KEY_POWER,          Control.POWER_FRY);
		withALTCodeMap.put(KEY_PAUSE,          Control.PAUSE);
		withALTCodeMap.put(KEY_FRAME,          Control.FRAME);
		withALTCodeMap.put(KEY_TRACE,          Control.TRACE);
		withALTCodeMap.put(KEY_DEBUG,          Control.DEBUG);
		withALTCodeMap.put(KEY_NO_COLLISIONS,  Control.NO_COLLISIONS);
		withALTCodeMap.put(KEY_VIDEO_STANDARD, Control.VIDEO_STANDARD);
	
		withCTRLCodeMap.put(KEY_STATE_0,  Control.SAVE_STATE_0); 
		withCTRLCodeMap.put(KEY_STATE_1,  Control.SAVE_STATE_1); 
		withCTRLCodeMap.put(KEY_STATE_2,  Control.SAVE_STATE_2); 
		withCTRLCodeMap.put(KEY_STATE_3,  Control.SAVE_STATE_3); 
		withCTRLCodeMap.put(KEY_STATE_4,  Control.SAVE_STATE_4); 
		withCTRLCodeMap.put(KEY_STATE_5,  Control.SAVE_STATE_5); 
		withCTRLCodeMap.put(KEY_STATE_6,  Control.SAVE_STATE_6); 
		withCTRLCodeMap.put(KEY_STATE_7,  Control.SAVE_STATE_7); 
		withCTRLCodeMap.put(KEY_STATE_8,  Control.SAVE_STATE_8); 
		withCTRLCodeMap.put(KEY_STATE_9,  Control.SAVE_STATE_9); 
		withCTRLCodeMap.put(KEY_STATE_10, Control.SAVE_STATE_10); 
		withCTRLCodeMap.put(KEY_STATE_11, Control.SAVE_STATE_11); 
		withCTRLCodeMap.put(KEY_STATE_12, Control.SAVE_STATE_12); 

		withALTCodeMap.put(KEY_STATE_0,  Control.LOAD_STATE_0); 
		withALTCodeMap.put(KEY_STATE_1,  Control.LOAD_STATE_1); 
		withALTCodeMap.put(KEY_STATE_2,  Control.LOAD_STATE_2); 
		withALTCodeMap.put(KEY_STATE_3,  Control.LOAD_STATE_3); 
		withALTCodeMap.put(KEY_STATE_4,  Control.LOAD_STATE_4); 
		withALTCodeMap.put(KEY_STATE_5,  Control.LOAD_STATE_5); 
		withALTCodeMap.put(KEY_STATE_6,  Control.LOAD_STATE_6); 
		withALTCodeMap.put(KEY_STATE_7,  Control.LOAD_STATE_7); 
		withALTCodeMap.put(KEY_STATE_8,  Control.LOAD_STATE_8); 
		withALTCodeMap.put(KEY_STATE_9,  Control.LOAD_STATE_9); 
		withALTCodeMap.put(KEY_STATE_10, Control.LOAD_STATE_10); 
		withALTCodeMap.put(KEY_STATE_11, Control.LOAD_STATE_11); 
		withALTCodeMap.put(KEY_STATE_12, Control.LOAD_STATE_12); 
	}

	public void initJoystickKeys() {
		joysticksCodeMap.clear();
		if (!p1ControlsMode) {
			joysticksCodeMap.put(Parameters.KEY_P0_LEFT,    Control.JOY0_LEFT); 
			joysticksCodeMap.put(Parameters.KEY_P0_UP,      Control.JOY0_UP); 
			joysticksCodeMap.put(Parameters.KEY_P0_RIGHT,   Control.JOY0_RIGHT); 
			joysticksCodeMap.put(Parameters.KEY_P0_DOWN,    Control.JOY0_DOWN); 
			joysticksCodeMap.put(Parameters.KEY_P0_BUTTON,  Control.JOY0_BUTTON); 
			joysticksCodeMap.put(Parameters.KEY_P0_BUTTON2, Control.JOY0_BUTTON); 
			joysticksCodeMap.put(Parameters.KEY_P1_LEFT,    Control.JOY1_LEFT); 
			joysticksCodeMap.put(Parameters.KEY_P1_UP,      Control.JOY1_UP); 
			joysticksCodeMap.put(Parameters.KEY_P1_RIGHT,   Control.JOY1_RIGHT); 
			joysticksCodeMap.put(Parameters.KEY_P1_DOWN,    Control.JOY1_DOWN); 
			joysticksCodeMap.put(Parameters.KEY_P1_BUTTON,  Control.JOY1_BUTTON); 
			joysticksCodeMap.put(Parameters.KEY_P1_BUTTON2, Control.JOY1_BUTTON);
		} else {
			joysticksCodeMap.put(Parameters.KEY_P0_LEFT,    Control.JOY1_LEFT); 
			joysticksCodeMap.put(Parameters.KEY_P0_UP,      Control.JOY1_UP); 
			joysticksCodeMap.put(Parameters.KEY_P0_RIGHT,   Control.JOY1_RIGHT); 
			joysticksCodeMap.put(Parameters.KEY_P0_DOWN,    Control.JOY1_DOWN); 
			joysticksCodeMap.put(Parameters.KEY_P0_BUTTON,  Control.JOY1_BUTTON); 
			joysticksCodeMap.put(Parameters.KEY_P0_BUTTON2, Control.JOY1_BUTTON); 
			joysticksCodeMap.put(Parameters.KEY_P1_LEFT,    Control.JOY0_LEFT); 
			joysticksCodeMap.put(Parameters.KEY_P1_UP,      Control.JOY0_UP); 
			joysticksCodeMap.put(Parameters.KEY_P1_RIGHT,   Control.JOY0_RIGHT); 
			joysticksCodeMap.put(Parameters.KEY_P1_DOWN,    Control.JOY0_DOWN); 
			joysticksCodeMap.put(Parameters.KEY_P1_BUTTON,  Control.JOY0_BUTTON); 
			joysticksCodeMap.put(Parameters.KEY_P1_BUTTON2, Control.JOY0_BUTTON);
		}
	}

	private ConsoleControlsInput consoleControlsInput; 
	private final VideoMonitor videoMonitor;

	private boolean p1ControlsMode = false;

	private boolean paddleMode = false;
	private int paddle0Position = 0;			// 380 = LEFT, 190 = MIDDLE, 0 = RIGHT
	private int paddle0Speed = 2;				// 1 to 10
	private boolean paddle0MovingLeft = false;
	private boolean paddle0MovingRight = false;
	private int paddle1Position = 0;
	private int paddle1Speed = 2;				
	private boolean paddle1MovingLeft = false;
	private boolean paddle1MovingRight = false;
	private PaddlesPositionUpdater paddlePositionUpdater;

	private final Map<Integer, ConsoleControls.Control> joysticksCodeMap = new HashMap<Integer, ConsoleControls.Control>();
	private final Map<Integer, ConsoleControls.Control> normalCodeMap = new HashMap<Integer, ConsoleControls.Control>();
	private final Map<Integer, ConsoleControls.Control> withCTRLCodeMap = new HashMap<Integer, ConsoleControls.Control>();
	private final Map<Integer, ConsoleControls.Control> withALTCodeMap = new HashMap<Integer, ConsoleControls.Control>();
	
	private final Map<ConsoleControls.Control, Boolean> controlStateMap = new HashMap<ConsoleControls.Control, Boolean>();

                                           
	private static final int KEY_POWER          = KeyEvent.VK_F1;     
	private static final int KEY_BLACK_WHITE    = KeyEvent.VK_F2;     
	private static final int KEY_DIFFICULTY0    = KeyEvent.VK_F4;  
	private static final int KEY_DIFFICULTY1    = KeyEvent.VK_F9;  
	private static final int KEY_SELECT         = KeyEvent.VK_F11;    
	private static final int KEY_RESET          = KeyEvent.VK_F12;    
                                               
	private static final int KEY_PAUSE          = KeyEvent.VK_P;
	private static final int KEY_FRAME          = KeyEvent.VK_F;
	private static final int KEY_TRACE          = KeyEvent.VK_T;
	private static final int KEY_FAST_SPEED     = KeyEvent.VK_TAB;
	private static final int KEY_DEBUG          = KeyEvent.VK_D;
	private static final int KEY_NO_COLLISIONS  = KeyEvent.VK_C;
	private static final int KEY_VIDEO_STANDARD = KeyEvent.VK_V;
	
	private static final int KEY_STATE_0        = KeyEvent.VK_QUOTE;
	private static final int KEY_STATE_1        = KeyEvent.VK_1;
	private static final int KEY_STATE_2        = KeyEvent.VK_2;
	private static final int KEY_STATE_3        = KeyEvent.VK_3;
	private static final int KEY_STATE_4        = KeyEvent.VK_4;
	private static final int KEY_STATE_5        = KeyEvent.VK_5;
	private static final int KEY_STATE_6        = KeyEvent.VK_6;
	private static final int KEY_STATE_7        = KeyEvent.VK_7;
	private static final int KEY_STATE_8        = KeyEvent.VK_8;
	private static final int KEY_STATE_9        = KeyEvent.VK_9;
	private static final int KEY_STATE_10       = KeyEvent.VK_0;
	private static final int KEY_STATE_11       = KeyEvent.VK_MINUS;
	private static final int KEY_STATE_12       = KeyEvent.VK_EQUALS;

	private static final int KEY_LOCAL_P1_MODE     = KeyEvent.VK_J;
	private static final int KEY_LOCAL_PADDLE_MODE = KeyEvent.VK_L;
	
	private static final Control[] playerDigitalControls = new Control[] {
		Control.JOY0_UP, Control.JOY0_DOWN, Control.JOY0_LEFT, Control.JOY0_RIGHT, Control.JOY0_BUTTON,
		Control.JOY1_UP, Control.JOY1_DOWN, Control.JOY1_LEFT, Control.JOY1_RIGHT, Control.JOY1_BUTTON,
		Control.PADDLE0_BUTTON, Control.PADDLE1_BUTTON
	};

	
	private class PaddlesPositionUpdater extends Thread {
		@Override
		public void run() {
			while(paddleMode) {
				paddlesUpdatePosition();
				try { sleep(1000 / 60);	} catch (InterruptedException e) {}   // Runs @ ~60Hz
			}
		}
	}

}
