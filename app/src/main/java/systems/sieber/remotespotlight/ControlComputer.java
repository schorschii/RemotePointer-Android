package systems.sieber.remotespotlight;

import java.util.Date;

class ControlComputer {
    String hostname;
    String address;
    Date recognized;

    ControlComputer(String hostname, String address, Date recognized) {
        this.hostname = hostname;
        this.address = address;
        this.recognized = recognized;
    }
}
