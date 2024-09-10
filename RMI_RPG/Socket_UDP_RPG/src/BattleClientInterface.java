import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BattleClientInterface extends Remote {
    void updateGameStatus(String status) throws RemoteException;
}
