import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleServerInterface extends Remote {
    String registerPlayer(String playerName, BattleClientInterface client) throws RemoteException;
    String playerAction(String clientKey, String action) throws RemoteException;
    String getGameStatus() throws RemoteException;
}
