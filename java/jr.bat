@echo off
jview /D:MFPTx=COM1:7158 /D:NOP=41 /D:REG=001 /D:SRV=999 class\%1 %2 %3 %4
