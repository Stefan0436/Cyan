# CCFG CQ, command line interface for the CCFG system
### Installation (linux):
1. run `sudo mkdir /usr/lib/ccfg-cq-libs`
2. copy CCFG's jar to /usr/lib/ccfg-cq-libs
3. copy [AOS-UTIL SLIB](https://aerialworks.ddns.net/maven/org/asf/aos/util/service/aosutil-service-SLIB-UTIL/0.0.0.2/aosutil-service-SLIB-UTIL-0.0.0.2.jar) jar to /usr/lib/ccfg-cq-libs
4. run `sudo cp cq /usr/bin/cq`
5. run `sudo chmod +x /usr/bin/cq`

### Installation (windows):
1. run `mkdir "C:\Program Files (x86)\CCFG-CQ-LIBS"`
2. copy CCFG's jar to C:\Program Files (x86)\CCFG-CQ-LIBS
3. copy [AOS-UTIL SLIB](https://aerialworks.ddns.net/maven/org/asf/aos/util/service/aosutil-service-SLIB-UTIL/0.0.0.13/aosutil-service-SLIB-UTIL-0.0.0.2.jar) jar to C:\Program Files (x86)\CCFG-CQ-LIBS
4. run `SETX /M PATH "%PATH%;C:\Program Files (x86)\CCFG-CQ-LIBS"`
5. copy the cq.bat file to C:\Program Files (x86)\CCFG-CQ-LIBS
