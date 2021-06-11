package com.cursoandroid.flappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

public class Game extends ApplicationAdapter {

	private SpriteBatch batch;
	private Texture[] birds;
	private Texture background;
	private Texture topPipe, bottomPipe;
	private Texture gameover;

	//atributos de configuração
	private float gravity = 0;
	private float initPositionY = 0, initPositionX = 0;
	private int movX = 0;
	private float deviceWidth, deviceHeight;
	private float var = 0;
	private float pipePosX, pipePosY;
	private float spacePipe;
	private Random random;
	private int score = 0;
	private int scoreMax = 0;
	private boolean isPassedPipe = false;
	private int status = 0;

	//sons do jogo
	private Sound flySound, collSound, scoreSound;

	//exibição de textos
	BitmapFont scoreText;
	BitmapFont restartText;
	BitmapFont bestScoreText;

	//criando formas para colisão(hitBox)
	private Circle birdHitbox;
	private Rectangle topPipeRect, bottomPipeRect;
	private ShapeRenderer shapeRenderer;
	private Rectangle topRect, bottomRect;

	//salvar pontuação
	private Preferences preferences;

	//objs camera
	private OrthographicCamera camera;
	private Viewport viewport;
	private final float VIRTUAL_WIDTH = 720;
	private final float VIRTUAL_HEIGHT = 1280;
	
	@Override
	public void create () {
		initTextures();
		initObjs();
	}

	@Override
	public void render () {

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		gameStatus();
		validScore();
		drawTextures();
		detectCollision();

	}
	
	@Override
	public void dispose () {

	}

	private void drawTextures(){

		batch.setProjectionMatrix(camera.combined);

		batch.begin();

		batch.draw(background, 0, 0, deviceWidth, deviceHeight);
		batch.draw(birds[(int)var], 50 + initPositionX, initPositionY, 140, 94);
		batch.draw(topPipe, pipePosX, deviceHeight/2 + spacePipe + pipePosY);
		batch.draw(bottomPipe, pipePosX, deviceHeight/2 - bottomPipe.getHeight() - spacePipe + pipePosY);
		scoreText.draw(batch, String.valueOf(score), VIRTUAL_WIDTH/2, deviceHeight - 100);

		if(status == 2){
			batch.draw(gameover, deviceWidth/2 - gameover.getWidth()/2, deviceHeight/2);
			restartText.draw(batch, "Toque para Reiniciar!", deviceWidth/2 - 140, deviceHeight/2 - gameover.getHeight()/2);
			bestScoreText.draw(batch, "RECORD: " + scoreMax,deviceWidth/2 - 70, deviceHeight/2 - gameover.getHeight());
		}

		batch.end();

	}

	private void initTextures(){

		birds = new Texture[3];

		for(int i = 0; i<3; i++){
			String internalPath = String.format("passaro%d.png", (i+1));
			birds[i] = new Texture(internalPath);
		}

		birds[0] = new Texture("passaro1.png");
		background = new Texture("fundo.png");
		topPipe = new Texture("cano_topo_maior.png");
		bottomPipe = new Texture("cano_baixo_maior.png");
		gameover = new Texture("game_over.png");

	}

	private void initObjs(){
		random = new Random();
		batch = new SpriteBatch();
		deviceWidth = VIRTUAL_WIDTH;
		deviceHeight = VIRTUAL_HEIGHT;
		initPositionY = deviceHeight/2;
		pipePosX = deviceWidth;
		spacePipe = 200;

		//configs dos textos
		scoreText = new BitmapFont();
		scoreText.setColor(Color.WHITE);
		scoreText.getData().setScale(10);
		restartText = new BitmapFont();
		restartText.setColor(Color.GREEN);
		restartText.getData().setScale(2);
		bestScoreText = new BitmapFont();
		bestScoreText.setColor(Color.RED);
		bestScoreText.getData().setScale(2);

		//formas geométricas para colisões
		shapeRenderer = new ShapeRenderer();
		birdHitbox = new Circle();
		topPipeRect = new Rectangle();
		bottomPipeRect = new Rectangle();
		topRect = new Rectangle();
		bottomRect = new Rectangle();

		//sons
		flySound = Gdx.audio.newSound(Gdx.files.internal("som_asa.wav"));
		collSound = Gdx.audio.newSound(Gdx.files.internal("som_batida.wav"));
		scoreSound = Gdx.audio.newSound(Gdx.files.internal("som_pontos.wav"));

		//preferencias
		preferences = Gdx.app.getPreferences("record");
		scoreMax = preferences.getInteger("scoreMax", 0);

		//camera
		camera = new OrthographicCamera();
		camera.position.set(VIRTUAL_WIDTH/2, VIRTUAL_HEIGHT/2, 0);
		viewport = new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);

	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
	}

	private void gameStatus(){
		boolean justTouched = Gdx.input.justTouched();
		if(status == 0){

			//aplicar click events na tela
			if(justTouched) {
				gravity = -15;
				status = 1;
				flySound.play();
			}
		}else if(status == 1){

			if(justTouched) {
				gravity = -15;
				flySound.play();
			}

			//movimento dos canos
			pipePosX -= Gdx.graphics.getDeltaTime() * 700;
			if(pipePosX < - bottomPipe.getWidth()){
				pipePosX = deviceWidth;
				pipePosY = random.nextInt(800) - 400;
				isPassedPipe = false;
			}

			if(initPositionY > 0 || justTouched)
				initPositionY = initPositionY - gravity;

			gravity++;

		}else if(status == 2){

			if(score > scoreMax){
				scoreMax = score;
				preferences.putInteger("scoreMax", scoreMax);
				preferences.flush();
			}

			initPositionX -= Gdx.graphics.getDeltaTime()*1500;

			if(justTouched) {
				status = 0;
				score = 0;
				gravity = 0;
				initPositionY = deviceHeight/2;
				initPositionX = 0;
				pipePosX = deviceWidth;
			}
		}

	}

	public void validScore(){

		if(pipePosX < 50) {
			if(!isPassedPipe) {
				score++;
				isPassedPipe = true;
				scoreSound.play();
			}
		}

		var += Gdx.graphics.getDeltaTime() * 10;

		if(var > 3){
			var = 0;
		}
	}

	public void detectCollision(){

		birdHitbox.set(50 + birds[0].getWidth() + initPositionX, initPositionY + birds[0].getHeight(),
				birds[0].getHeight());
		topPipeRect.set(pipePosX, deviceHeight/2 + spacePipe + pipePosY,
				topPipe.getWidth(), topPipe.getHeight());
		bottomPipeRect.set(pipePosX, deviceHeight/2 - bottomPipe.getHeight() - spacePipe + pipePosY,
				bottomPipe.getWidth(), bottomPipe.getHeight());
		topRect.set(0,deviceHeight+10,deviceWidth,10);
		bottomRect.set(0,0,deviceWidth,10);

		boolean isCollTopPipe = Intersector.overlaps(birdHitbox, topPipeRect);
		boolean isCollBttnPipe = Intersector.overlaps(birdHitbox, bottomPipeRect);
		boolean isCollTop = Intersector.overlaps(birdHitbox, topRect);
		boolean isCollBttn = Intersector.overlaps(birdHitbox, bottomRect);

		if(isCollTopPipe || isCollBttnPipe) {
			if(status == 1){
				status = 2;
				collSound.play();
			}
		}


		if(isCollTop || isCollBttn){
			if(status == 1){
				status = 2;
				collSound.play();
			}
		}



		//Desenhando formas para teste de colisão
		/*
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		shapeRenderer.circle(50 + birds[0].getWidth(), initPositionY + birds[0].getHeight(), birds[0].getHeight());
		shapeRenderer.rect(pipePosX, deviceHeight/2 + spacePipe + pipePosY,
				topPipe.getWidth(), topPipe.getHeight());
		shapeRenderer.rect(pipePosX, deviceHeight/2 - bottomPipe.getHeight() - spacePipe + pipePosY,
				bottomPipe.getWidth(), bottomPipe.getHeight());

		shapeRenderer.rect(0,deviceHeight-10,deviceWidth,10);
		shapeRenderer.rect(0,10,deviceWidth,10);

		shapeRenderer.end();
		*/


	}
}
