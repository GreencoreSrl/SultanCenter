package com.ncr;

import java.util.Map;

import com.ncr.gpe.GpeResultProcessorInterface;

public class PosGPEAckReceived implements GpeResultProcessorInterface {

	public void processResult(Map messageMap) {

		PosGPE.sts = 5;
	}
}
