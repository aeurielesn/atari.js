// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package general.m6502.instructions;

import general.m6502.M6502;
import general.m6502.Instruction;

public class PHP extends Instruction {

	public PHP(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {
		return 3;
	}

	@Override
	public void execute() {
		cpu.pushByte(cpu.PS());
	}
	

	public static final long serialVersionUID = 1L;

}
