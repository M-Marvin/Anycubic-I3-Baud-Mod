**Modifaction to orignial Anycubic I3 Firmware**
---
This is an modification to the original Anycubic I3 3D-Printer Firmware v1.4.0RC23/P2 running ont the mainboard Trigorilla Pro v1.2, to which there sadly seems to be almost no documentation.
The Modification changes the baud rate of the printer from 250.000 to 230.400 to make it more compatible with Linux systems, specifically on the Raspberry Pi.

How I did it
---
In case someone finds it helpfull, here is how I did it:

1. Unplug the printer from both power and USB, remove the Jumper `JP1` right besides the STM32F103 MCU, this will remove the pull down for the BOOT0 Pin and enable the STM boot loader.
2. Optionally change the Jumper labled with `5V USB` to `USB` to allow the board to be powered from USB only, without the need to power the entire printer, the board can even be removed from the printer this way.
3. Connect the board to the PC using USB, check which COM Port it got in the windows settings, this requires an system which can deal with the strange 250.000 baud rate.
4. Download the current firmware image using STM's `Flash Loader Demonstartor` Tool, which can be downloaded on the STM website for free, make sure to select the 512K flash version of the Chip to download all of the firmware.
5. Convert the downloaded S-Record file `.s19` into an binary image file `.bin`, I wrote an quick java code `s19-to-bin.java` to do that for me, but other tools might work too.
6. Load the firmware binary into Ghridra, which can also be downloaded for free, make sure to select the correct architecture for the STM32 MCU, which is `ARM 32 LE`
7. Revers engineering the firmware:<br/>
Here the tricky part begins, trough the programming manual I found out which register is responsible for setting the baud rate of the `UART1` interface of the MCU, which is connected to the UART-USB IC, and at whcih address it is: Register for `UART1` Baud rate: `BRR` (Baud Rate Register) located at `0x40013808`<br/>
Trough some extensive searching in the assembly and decompiled code in Ghidra, I managed to find the HAL function which configures that register, and trace back the call path up to the function holding the baud rate which is configured.<br/>
I also uploaded the Ghridra project, where I labled all those functions, variables, parameters and the global constant which holds the default baud `DAT_UART1_BAUD` at `0x080286f8` with default value `0x0003D090` aka `250000`<br/>
This baud rate can probably be changed to everything the STM32 supports, I decided to change it to the closest supported one, `230.400`, the change can be done using any hex editor.<br/>
8. Now convert the edited binary firmware back to an S-Record file, I again used the java code I wrote for that, but other tools should work too.<br/>
9. Re-flash the firmware back onto the mainboard using the same flasher tool as before, make sure to compare at least the first few lines of the new firmware file with the original one, I dont know what might happen if an broken firmware is being flashed.<br/>
10. Finally, reset all jumpers back to their original position, and try to boot up the printer, it should now respond on the newly configured baud.<br/>

Background on why I did this
---
I recently wanted to use my Anycubic I3 3D-Printer with an Linux system, specifically an Raspberry Pi, and it was a huge problem that the Printer used an really absurd baud rate of 250.000 Bit/s since this is not supported by linux, at least not on an Raspberry Pi.
After googling a loot and even contacting Anycubic, it seemed to be clear that there is no way to get the source code for the Firmware of the Printers Mainboard.

There are projects and sourceode for older mainboard versions of the I3, but not the most recent Trigorilla Pro v1.2 with the FW version 1.4.0RC23/P2.
So I decided to extract and dissassemble the firmware of the printer and modify it on machinecode loevel to use the more compatible baud rate of 230.400 Bits/s.
