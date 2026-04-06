class CentralHub {
    public void registerDevice(SmartLight light) {
        System.out.println("[HUB] Dang ket noi voi thiet bi: " + light.getName());
    }
}
class SmartLight{
    private String id, name;
    private int brightness;
    public SmartLight(String id, String name, int brightness){
        this.name = name;
        this.id = id;
        this.brightness = brightness;
    }
    public SmartLight(String id, String name){
        this.name = name;
        this.id = id;
        this.brightness = 50;
    }
    public String getName(){
        return this.name;
    }
    public void setBrightness(int brightness){
        this.brightness =brightness;
    }
    public void setBrightness(String modeSet){
        if(modeSet.equals("MAX")){
            this.brightness = 100;
        }
        else if(modeSet.equals("ECO")){
            this.brightness = 30;
        }
        else if (modeSet.equals("MIN")){
            this.brightness = 10;
        }
    }
    public void getBright(){
        System.out.println("Do sang: "+ this.brightness);
    }
    public void connectToHub(CentralHub hub){
        hub.registerDevice(this);
    }
    
}
public class Bai20{
    public static void main(String[] args){
        CentralHub hub = new CentralHub();
        SmartLight l1 = new SmartLight("L01","Den phong khach",80 );
        SmartLight l2 = new SmartLight("L02","Den ngu" );
        l2.setBrightness("ECO");
        l1.connectToHub(hub);
        l2.connectToHub(hub);
        l1.getBright();
        l2.getBright();
    }
}