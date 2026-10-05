@echo off
title WishPlay Android Emulator
cd /d "C:\Users\mdlia\AppData\Local\Android\Sdk\emulator"
set ANDROID_HOME=C:\Users\mdlia\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\mdlia\AppData\Local\Android\Sdk
start "" "C:\Users\mdlia\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd lockout_qa
