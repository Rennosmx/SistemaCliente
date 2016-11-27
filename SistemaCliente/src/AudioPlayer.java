import java.io.InputStream;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

public class AudioPlayer extends Thread
{
    private InputStream in;
    private AdvancedPlayer player;
    private long duracao;
    private Thread playerThread;
    public AudioPlayer(InputStream in, long duracao) {
      this.in = in;
      this.duracao = duracao;
      playerThread = this;
    }
    
    public AudioPlayer(InputStream in) {
      this(in, -1);
    }
    
    private void play() {
      try {
        player = new AdvancedPlayer(in, javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice());
      } catch (JavaLayerException e) {
        e.printStackTrace();
      }
      
      Thread timer = new Thread(new Runnable() {
        @SuppressWarnings("deprecation")
        @Override
        public void run() {
          try {
            Thread.sleep(duracao);
          } catch (InterruptedException e) {
          }
          playerThread.stop();
        }
      });

      try {
        if (duracao > 0 ) {
          timer.start();
        }        
        player.play();
      } catch (JavaLayerException e) {
        e.printStackTrace();
      }
    }

    public void run() {
      play();
    }

    public AdvancedPlayer getPlayer() {
      return player;
    }

    public long getDuracao() {
      return duracao;
    }
  
}