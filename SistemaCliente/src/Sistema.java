import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Sistema {
  //////////////////////////////////////////////////////////////
  public static final int PORTA_PRODUTORA_MUSICA = 50000;
  public static final String IP_PRODUTORA_MUSICA = "127.0.0.1";
  //////////////////////////////////////////////////////////////

  private static final String HELP_MESSAGE = "\n"
      + "  help - mostra a lista de comandos.\n"
      + "  logar [login] [senha] - loga no sistema\n"     
      + "  cadastrar [login] [senha] [numeroCartao] - cadastra no sistema\n"      
      + "  listar - lista as musicas à venda\n"      
      + "  play [codigoMusica] - toca a música do código informado\n"      
      + "  stop  - para de tocar\n"      
      + "  listar - lista as musicas à venda\n"      
      + "  compras - lista as musicas compradas\n"      
      + "  comprar [codigoMusica] - compra a música do código informado\n"       
      + "  baixar [codigoMusica] - faz o download da música do código informado\n\n";
  
  private static final int TEMPO_REPRODUCAO = 30;     

  private String login;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private Socket socket;
  private AudioPlayer audioPlayer;
  
  public void iniciar() {
    System.out.println("CLIENTE v1.0");
    System.out.println("*** ATENÇÂO VOCÊ DEVE LOGAR ANTES DE USAR ALGUNS COMANDOS!!!!***");
    System.out.println("*** DIGITE HELP PARA VER OS COMANDOS DISPONÍVEIS ***");
    System.out.println("");
    
    while (true) {
      System.out.print("> ");
      System.out.flush();
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      String line = "";
      try {
        line = reader.readLine();
      } catch (IOException e) {
        e.printStackTrace();
      }
      processarComando(line);
    }
  }

  private void processarComando(String readLine) {
    String[] tokens = readLine.split(" ");
    
    if (tokens.length == 0) {
      System.out.println("[ERRO] Comando inválido");
      return;
    }
    
    String comando = tokens[0];
    
    if (comando.equalsIgnoreCase("help") || comando.equals("?")) {
      System.out.println(HELP_MESSAGE);
      
      return;
    }    
    
    if (comando.equalsIgnoreCase("logar")) {
      if (login != null) {
        System.out.println("[ERRO] Usuário já está autenticado!");       
        return;
      }     
      if (tokens.length != 3) {
        System.out.println("[ERRO] Deve ser: logar [login] [senha]");       
        return;
      }

      logar(tokens[1], tokens[2]);
      return;
    }
    
    if (comando.equalsIgnoreCase("sair")) {
      sair();
      return;
    }
    
    if (comando.equalsIgnoreCase("cadastrar")) {
      if (tokens.length != 4) {
        System.out.println("[ERRO] Deve ser: cadastrar [login] [senha] [numeroCartao]");       
        return;
      }     
      
      cadastrar(tokens[1], tokens[2], tokens[3]);
      return;
    }
    
    if (comando.equalsIgnoreCase("listar")) {
      if (login == null) {
        System.out.println("[ERRO] Usuário deve logar primeiro no sistema!");       
        return;
      }     
      
      listarMusicas();
      return;
    }
    
    if (comando.equalsIgnoreCase("compras")) {
      if (login == null) {
        System.out.println("[ERRO] Usuário deve logar primeiro no sistema!");       
        return;
      }     
      
      listarMusicasCompradas();
      return;
    }  
    
    if (comando.equalsIgnoreCase("baixar")) {
      if (login == null) {
        System.out.println("[ERRO] Usuário deve logar primeiro no sistema!");       
        return;
      }     
      
      if (tokens.length != 2) {
        System.out.println("[ERRO] Deve ser: baixar [codigo da musica]");      
        return;
      }
      
      int codigo;
      try {
        codigo = Integer.parseInt(tokens[1]);
      } catch (NumberFormatException e){
        System.out.println("[ERRO] Código da música inválido!");      
        return;        
      }
      
      baixarMusica(codigo);
      return;
    }
    
    if (comando.equalsIgnoreCase("play")) {
      if (login == null) {
        System.out.println("[ERRO] Usuário deve logar primeiro no sistema!");       
        return;
      }     
      
      if (tokens.length != 2) {
        System.out.println("[ERRO] Deve ser: play [codigo da musica]");      
        return;
      }
      
      int codigo;
      try {
        codigo = Integer.parseInt(tokens[1]);
      } catch (NumberFormatException e){
        System.out.println("[ERRO] Código da música inválido!");      
        return;        
      }
      
      playMusica(codigo);
      return;
    }
    
    if (comando.equalsIgnoreCase("stop")) {
      if (audioPlayer == null || !audioPlayer.isAlive()) {
        System.out.println("[ERRO] Nenhuma música está sendo reproduzida!");       
        return;
      }     
 
      stopMusica();
      return;
    }
    
    if (comando.equalsIgnoreCase("comprar")) {
      if (login == null) {
        System.out.println("[ERRO] Usuário deve logar primeiro no sistema!");       
        return;
      }     
      
      if (tokens.length != 2) {
        System.out.println("[ERRO] Deve ser: comprar [codigo da musica]");      
        return;
      }
      
      int codigo;
      try {
        codigo = Integer.parseInt(tokens[1]);
      } catch (NumberFormatException e){
        System.out.println("[ERRO] Código da música inválido!");      
        return;        
      }
      
      if (comprarMusica(codigo)) {
        System.out.println("Deseja baixá-la agora? (S/N) ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
          char c = (char) reader.read();
          if (c == 'S' || c == 's') {
            baixarMusica(codigo);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        
      }
      
      return;      
    }
    
    // comando inexistente
    System.out.println(HELP_MESSAGE);

    
  }
  
  private void playMusica(int codigo) {
    System.out.println("Preparando reprodução...");
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setTipo(ClientRequest.Tipo.PLAY);
    clientRequest.setLogin(login);
    clientRequest.setParameters(new String[]{codigo + ""});

    conectar();
    Response response = new Response();
    ByteArrayOutputStream dataOut = null;
    File file = null;
    boolean musicaComprada = false;

    try {
  
      out.writeObject(clientRequest);
      out.flush();
      
      response = (Response) in.readObject(); // checa se a musica ja foi comprada
      musicaComprada = response.getData()[0] == 1;
          
      dataOut = new ByteArrayOutputStream();
      
      while (true) {
        response = (Response) in.readObject();
        if (response == null || !response.isSuccess() || response.getData() == null || response.getData().length == 0) {
          break;
        }
        dataOut.write(response.getData());
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      desconectar();
      if (response == null || !response.isSuccess()) {
        if (file != null) {
          file.delete();
        }
        
        System.out.println(
            "[ERRO] " + (response != null ? response.getErrorMessage() : "Erro desconhecido."));             
      } else {
    	if (musicaComprada) {
    		System.out.println("Reproduzindo a música. Use o comando 'stop' se quiser parar de tocar.");
    	} else {
    		System.out.println("Reproduzindo a música " + "por " + TEMPO_REPRODUCAO + " segundos. Use o comando 'stop' se quiser parar de tocar.");
    		
    	}
        stopMusica();
        audioPlayer = new AudioPlayer(new ByteArrayInputStream(dataOut.toByteArray()), musicaComprada ? -1 : TEMPO_REPRODUCAO*1000);
        audioPlayer.start();
      }
    }
  
  }
  
  @SuppressWarnings("deprecation")
  private void stopMusica() {
    if (audioPlayer != null && audioPlayer.isAlive()) {
      audioPlayer.stop();
      audioPlayer = null;
    }
  }
  
  private void baixarMusica(int codigo) {
    System.out.println("Solicitando download...");
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setTipo(ClientRequest.Tipo.BAIXAR);
    clientRequest.setLogin(login);
    clientRequest.setParameters(new String[]{codigo + ""});

    conectar();
    Response response = new Response();
    FileOutputStream fileOut = null;
    File file = null;
    
    try {
      out.writeObject(clientRequest);
      out.flush();
      
      file = new File(System.getProperty("user.home") + File.separator + codigo + ".mp3");
      fileOut = new FileOutputStream(file);
      
      System.out.println("Baixando...");
      while (true) {
        response = (Response) in.readObject();
        if (response == null || !response.isSuccess() || response.getData() == null || response.getData().length == 0) {
          break;
        }
        fileOut.write(response.getData());
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      desconectar();
      if (fileOut != null) {
        try {
          fileOut.close();
          fileOut = null;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (response == null || !response.isSuccess()) {
        if (file != null) {
          file.delete();
        }
        
        System.out.println(
            "[ERRO] " + (response != null ? response.getErrorMessage() : "Erro desconhecido."));             
      } else {
        System.out.println("Arquivo baixado com sucesso para: " + file.getAbsolutePath());
      }
    }
  
  }

  private boolean comprarMusica(int codigo) {
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setTipo(ClientRequest.Tipo.COMPRAR);
    clientRequest.setLogin(login);
    clientRequest.setParameters(new String[]{codigo + ""});

    conectar();
    Response response = new Response();
    
    try {
      System.out.println("Processando compra...");
      out.writeObject(clientRequest);
      out.flush();
      response =  (Response) in.readObject();
      
      if (response == null || !response.isSuccess()) {
          System.out.println(
              "[ERRO] " + (response != null ? response.getErrorMessage() : "Erro desconhecido."));          
      } else {
        System.out.println("Compra da música " + codigo + " realizada com sucesso!");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      desconectar();
    }
  
    return response.isSuccess();
  }
  

  @SuppressWarnings("unchecked")
  private void listarMusicas() {
    System.out.println("Obtendo lista de músicas disponíveis para compra...");
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setTipo(ClientRequest.Tipo.LISTAR);
    clientRequest.setLogin(login);
    conectar();
    List<Musica> response = new ArrayList<>();
    try {
      out.writeObject(clientRequest);
      out.flush();
      response =  (List<Musica>) in.readObject();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      desconectar();
    }

    if (response.isEmpty()) {
      System.out.println("Nenhuma música disponível à venda.");
      return;
    }
    
    String[] headers = new String[] {
        "CÓDIGO",
        "PREÇO",
        "NOME",
        "ARTISTA",
        "ÁLBUM",
        "GÊNERO"
    };

    int[] maxLengths = new int[] {
        headers[0].length(),
        headers[1].length(),
        headers[2].length(),
        headers[3].length(),
        headers[4].length(),
        headers[5].length()    
    };
    
    for (Musica musica : response) {
      if (musica.getAlbum() == null) {
        musica.setAlbum("");
      }
      
      if (musica.getAno() == null) {
        musica.setAno("");
      }
      
      if (musica.getArtista() == null) {
        musica.setArtista("");
      }
      
      if (musica.getGenero() == null) {
        musica.setGenero("");
      }
      
      if (musica.getNome() == null) {
        musica.setNome("");
      }
      
      if (maxLengths[0] < Integer.toString(musica.getCodigo()).length()) {
        maxLengths[0] = Integer.toString(musica.getCodigo()).length();
      }
      
      if (maxLengths[1] < Double.toString(musica.getPreco()).length()) {
        maxLengths[1] = Double.toString(musica.getPreco()).length();
      }
      
      if (maxLengths[2] < musica.getNome().length()) {
        maxLengths[2] = musica.getNome().length();
      }
      
      if (maxLengths[3] < musica.getArtista().length()) {
        maxLengths[3] = musica.getArtista().length();
      }
      
      if (maxLengths[4] < musica.getAlbum().length()) {
        maxLengths[4] = musica.getAlbum().length();
      }
      
      if (maxLengths[5] < musica.getGenero().length()) {
        maxLengths[5] = musica.getGenero().length();
      }
       
    }
    
    System.out.println(
                 rightFillWithSpaces(headers[0], maxLengths[0])
        + "  " + rightFillWithSpaces(headers[1], maxLengths[1])
        + "  " + rightFillWithSpaces(headers[2], maxLengths[2])
        + "  " + rightFillWithSpaces(headers[3], maxLengths[3])
        + "  " + rightFillWithSpaces(headers[4], maxLengths[4])
        + "  " + rightFillWithSpaces(headers[5], maxLengths[5])
    );
      
    for (Musica musica : response) {
      
        System.out.println(
                     rightFillWithSpaces(musica.getCodigo() + "", maxLengths[0]) 
            + "  " + rightFillWithSpaces(musica.getPreco()  + "", maxLengths[1])
            + "  " + rightFillWithSpaces(musica.getNome()    + "", maxLengths[2])
            + "  " + rightFillWithSpaces(musica.getArtista() + "", maxLengths[3])
            + "  " + rightFillWithSpaces(musica.getAlbum()   + "", maxLengths[4])
            + "  " + rightFillWithSpaces(musica.getGenero()  + "", maxLengths[5])
         );
    }
  }
  
  @SuppressWarnings("unchecked")
  private List<Musica> fetchMusicasCompradas() {
	  ClientRequest clientRequest = new ClientRequest();
	  clientRequest.setTipo(ClientRequest.Tipo.LISTAR_COMPRADOS);
	  clientRequest.setLogin(login);
	  conectar();
	  List<Musica> response = new ArrayList<>();
	  try {
	    out.writeObject(clientRequest);
	    out.flush();
	    response =  (List<Musica>) in.readObject();
	  } catch (IOException e) {
	    e.printStackTrace();
	  } catch (ClassNotFoundException e) {
	    e.printStackTrace();
	  } finally {
	    desconectar();
	  }
	  
	  return response;
  }
  
  private void listarMusicasCompradas() {
    System.out.println("Obtendo a lista de músicas já compradas...");

    List<Musica> response = fetchMusicasCompradas();
    
    if (response.isEmpty()) {
      System.out.println("Você ainda não comprou nada.");
      return;
    }
    
    String[] headers = new String[] {
        "CÓDIGO",
        "NOME",
        "ARTISTA",
        "ÁLBUM",
        "GÊNERO"
    };

    int[] maxLengths = new int[] {
        headers[0].length(),
        headers[1].length(),
        headers[2].length(),
        headers[3].length(),
        headers[4].length(),        
    };
    
    for (Musica musica : response) {
      if (musica.getAlbum() == null) {
        musica.setAlbum("");
      }
      
      if (musica.getAno() == null) {
        musica.setAno("");
      }
      
      if (musica.getArtista() == null) {
        musica.setArtista("");
      }
      
      if (musica.getGenero() == null) {
        musica.setGenero("");
      }
      
      if (musica.getNome() == null) {
        musica.setNome("");
      }
      
      if (maxLengths[0] < Integer.toString(musica.getCodigo()).length()) {
        maxLengths[0] = Integer.toString(musica.getCodigo()).length();
      }
      
      if (maxLengths[1] < musica.getNome().length()) {
        maxLengths[1] = musica.getNome().length();
      }
      
      if (maxLengths[2] < musica.getArtista().length()) {
        maxLengths[2] = musica.getArtista().length();
      }
      
      if (maxLengths[3] < musica.getAlbum().length()) {
        maxLengths[3] = musica.getAlbum().length();
      }
      
      if (maxLengths[4] < musica.getGenero().length()) {
        maxLengths[4] = musica.getGenero().length();
      }
       
    }
    
    System.out.println(
                 rightFillWithSpaces(headers[0], maxLengths[0])
        + "  " + rightFillWithSpaces(headers[1], maxLengths[1])
        + "  " + rightFillWithSpaces(headers[2], maxLengths[2])
        + "  " + rightFillWithSpaces(headers[3], maxLengths[3])
        + "  " + rightFillWithSpaces(headers[4], maxLengths[4])
    );
      
    for (Musica musica : response) {
      
        System.out.println(
                     rightFillWithSpaces(musica.getCodigo() + "", maxLengths[0]) 
            + "  " + rightFillWithSpaces(musica.getNome()    + "", maxLengths[1])
            + "  " + rightFillWithSpaces(musica.getArtista() + "", maxLengths[2])
            + "  " + rightFillWithSpaces(musica.getAlbum()   + "", maxLengths[3])
            + "  " + rightFillWithSpaces(musica.getGenero()  + "", maxLengths[4])
         );
    }
    
  }

  private void cadastrar(String login, String senha, String numeroCartao) {
    System.out.println("Efetuando cadastro do usuário...");
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setTipo(ClientRequest.Tipo.CADASTRAR);
    clientRequest.setParameters(new String[]{login, senha, numeroCartao});
    
    conectar();

    try {
      out.writeObject(clientRequest);
      out.flush();
      Response response = (Response) in.readObject();
      if (response.isSuccess()) {
        
      } else {
        System.out.println("[ERRO] " + response.getErrorMessage());  
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      desconectar();
    }
    
    System.out.println("Usuário cadastrado com sucesso!");
    
  }
  
  public void logar(String login, String senha) {
    System.out.println("Efetuando login do usuário...");
    ClientRequest clientRequest = new ClientRequest();
    clientRequest.setTipo(ClientRequest.Tipo.LOGAR);
    clientRequest.setParameters(new String[]{login, senha});
    
    conectar();

    try {
      out.writeObject(clientRequest);
      out.flush();
      Response response = (Response) in.readObject();
      if (response.isSuccess()) {
        this.login = login;
      } else {
        System.out.println(response.getErrorMessage());  
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      desconectar();
    }
    
    System.out.println("Usuário autenticado com sucesso!");

  }
   
  public void conectar() {
    if (socket != null) {
      return;
    }
    
    try {
      socket = new Socket(IP_PRODUTORA_MUSICA, PORTA_PRODUTORA_MUSICA);
      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());    
    } catch (UnknownHostException e) {
      System.err.println("UnknownHostException: " + e.getMessage());
    } catch (IOException e) {
      System.err.println("IOException: " + e.getMessage());  
    }
  }
  private void desconectar() {
    
    try {
      if (socket != null) {
        socket.close();
        socket = null;
      }
      
      if (in != null) {
        in.close();
        in = null;
      }
      
      if (out != null) {
        out.close();
        in = null;
      }    
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sair() {
    desconectar();
    System.exit(0);
  }
  
  public String leftFillWithSpaces(String str, int n) {
    StringBuilder buffer = new StringBuilder();
    int spacesCount = n - str.length();
    while (spacesCount > 0) {
      buffer.append(' ');
      --spacesCount;
    }   
    return buffer.append(str).toString();    
  }
  
  public String rightFillWithSpaces(String str, int n) {
    StringBuilder buffer = new StringBuilder(str);
    int spacesCount = n - str.length();   
    while (spacesCount > 0) {
      buffer.append(' ');
      --spacesCount;
    }  
    return buffer.toString();    
  }  
}
