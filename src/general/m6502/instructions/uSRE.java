// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package general.m6502.instructions;

import general.m6502.M6502;
import general.m6502.OperandType;
import general.m6502.UndocumentedInstruction;

public class uSRE extends UndocumentedInstruction {

	public uSRE(M6502 cpu, OperandType type) {
		super(cpu);
		this.type = type;
	}

	@Override
	public int fetch() {
		switch (type) {
			case Z_PAGE:
				ea = cpu.fetchZeroPageAddress(); return 5;
			case Z_PAGE_X:
				ea = cpu.fetchZeroPageXAddress(); return 6;
			case ABS:
				ea = cpu.fetchAbsoluteAddress();  return 6;
			case ABS_X:
				ea = cpu.fetchAbsoluteXAddress(); return 7;
			case ABS_Y:
				ea = cpu.fetchAbsoluteYAddress(); return 7;
			case IND_X:
				ea = cpu.fetchIndirectXAddress(); return 8;
			case IND_Y:
				ea = cpu.fetchIndirectYAddress(); return 8;
			default:
				throw new IllegalStateException("uSRE Invalid Operand Type: " + type);
		}
	}

	@Override
	public void execute() {
		byte val = cpu.memory.readByte(ea); 
		cpu.CARRY = (val & 0x01) != 0;		// bit 0 was set
		val = (byte) ((val & 0xff) >>> 1);
		cpu.memory.writeByte(ea, val);

		val = (byte) (cpu.A ^ val);
		cpu.A = val;
		cpu.ZERO = val == 0;
		cpu.NEGATIVE = val < 0;
	}

	private final OperandType type;
	
	private int ea;
	

	public static final long serialVersionUID = 1L;

}
