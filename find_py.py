import sys
import subprocess
import platform

ver = sys.version_info[0]
if (ver == 3):
    print(sys.executable)
elif (ver == 2):
	try:
		ver = subprocess.Popen(["python3", "-V"], stdout=subprocess.PIPE)
	except Exception:
		print("END Python 3 not installed! Install Python 3: (https://www.python.org/downloads/)!")
	else:
		if (ver.communicate()[0] == b''):
			print("END Python 3 not installed! Install Python 3: (https://www.python.org/downloads/)!")
		else:
			os = platform.system()
			if(os == "Darwin" or os == "Linux"):
				loc = subprocess.Popen(["which", "python3"], stdout=subprocess.PIPE)
			elif (os == "Windows"):
				loc = subprocess.Popen(["where", "python3"], stdout=subprocess.PIPE)
			else:
				print("END Unsupported os!")
			print(loc.communicate()[0].decode("utf-8").strip())