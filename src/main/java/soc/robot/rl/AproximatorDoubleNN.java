package soc.robot.rl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.primitives.Pair;

import fasttester.BotServer;

public class AproximatorDoubleNN implements Runnable {

	protected MultiLayerNetwork currentNetwork;
	
	protected MultiLayerNetwork targetNetwork;
	
	protected int id;
	
	protected boolean isTraining;
	
//	final private IExpReplay<Integer> expReplay;
	
	protected int expReplaySize;
	
	protected CircularFifoQueue< StateTransition > expReplay;
	
	protected int batchSize;
	
	protected double errorClamp;
	
	protected double gamma;
	
	protected int modelSaveFreq;
	
	protected int inputsize;
	
	protected int counter;
	
	protected double nnLearningRate;
	
	protected int hiddenLayers;
	
	protected int[] hiddenLayersNeurons;
	
	protected Activation outputLayerActivation;
	
	protected Random rnd;
	
	protected boolean tdLambda;
	
	public void setIsTraining(boolean is) {
		isTraining = is;
	}
	
	public AproximatorDoubleNN(int i, int inputsize) {
		id = i;
		this.inputsize = inputsize;
		readProperties();
		
		expReplay = new CircularFifoQueue<>(expReplaySize); /*maxSize*/
		currentNetwork = new MultiLayerNetwork(createConf()); 
		currentNetwork.init();
		targetNetwork = currentNetwork.clone();
		counter=0;
		isTraining = false;
		rnd = new Random();
		rnd.setSeed(BotServer.SEED);
	}
	
	protected void readProperties() {
		Properties pr = new Properties();
		expReplaySize = 20000;
		batchSize = 64;
		gamma = 1;
		modelSaveFreq = 1;
		errorClamp = 1;
		nnLearningRate = 0.002;
		hiddenLayers = 1;
		hiddenLayersNeurons = new int[hiddenLayers];
		hiddenLayersNeurons[0] = 40;
		outputLayerActivation = Activation.SIGMOID;
		tdLambda = false;
		
		try
        {
            final File pf = new File("AproximatorNN.properties");
            if (pf.exists())
            {
                if (pf.isFile() && pf.canRead())
                {
                    System.err.println("Reading startup properties from AproximatorNN.properties");
                    FileInputStream fis = new FileInputStream(pf);
                    pr.load(fis);
                    fis.close();
                    
                    String prTdLambda = pr.getProperty("tdLambda");
                    if(prTdLambda!=null) {
                    	this.tdLambda = Boolean.parseBoolean(prTdLambda);
                    }
                    
                    String inputSize = pr.getProperty("inputSize");
                    if(inputSize!=null) {
                    	this.inputsize = Integer.parseInt(inputSize);
                    }
                    
                    String prExpReplaySize = pr.getProperty("expReplaySize");
                    if(prExpReplaySize!=null) {
                    	expReplaySize = Integer.parseInt(prExpReplaySize);
                    }
                    
                    String prBatchSize = pr.getProperty("batchSize");
                    if(prBatchSize!=null) {
                    	batchSize = Integer.parseInt(prBatchSize);
                    } 
                    
                    String prGamma = pr.getProperty("gamma");
                    if(prGamma!=null) {
                    	gamma = Double.parseDouble(prGamma);
                    } 
                    
                    String prErrorClamp = pr.getProperty("errorClamp");
                    if(prErrorClamp!=null) {
                    	errorClamp =  Double.parseDouble(prErrorClamp);
                    } 
                    
                    String prModelSaveFreq = pr.getProperty("modelSaveFreq");
                    if(prModelSaveFreq!=null) {
                    	modelSaveFreq = Integer.parseInt(prModelSaveFreq);
                    } 
                    
                    String prNNLearningRate = pr.getProperty("nnLearningRate");
                    if(prNNLearningRate!=null) {
                    	nnLearningRate = Double.parseDouble(prNNLearningRate);
                    } 
                    
                    String prHiddenLayers = pr.getProperty("hiddenLayers");
                    if(prHiddenLayers!=null) {
                    	hiddenLayers = Integer.parseInt(prHiddenLayers);
                    } 
                    
                    hiddenLayersNeurons = new int[hiddenLayers];
                    for (int i=1; i<=hiddenLayers; i++) {
                    	String prHiddenLayerNeurons = pr.getProperty("hl" + i);
                        if(prHiddenLayerNeurons!=null) {
                        	hiddenLayersNeurons[i-1] = Integer.parseInt(prHiddenLayerNeurons);
                        } else {
                        	hiddenLayersNeurons[i-1] = 40;
                        }
                    }
                    
                    String prOutputLayerActivation = pr.getProperty("outputActivation");
                    if ("identity".equals(prOutputLayerActivation)) {
                    	outputLayerActivation = Activation.IDENTITY;
                    } 
                    
                } else {
                    System.err.println
                        ("*** Properties file  AproximatorNN.properties"
                         + " exists but isn't a readable plain file: Exiting.");
                    System.exit(1);
                }
            } else {
            	System.err.println
                ("No properties file found. Reading in defaults");
            }
        }
        catch (Exception e)
        {
            // SecurityException from .exists, .isFile, .canRead
            // IOException from FileInputStream construc [FileNotFoundException], props.load
            // IllegalArgumentException from props.load (malformed Unicode escape)
            System.err.println
                ("*** Error reading properties file  AproximatorNN.properties" 
                 + ", exiting: " + e.toString());
            if (e.getMessage() != null)
                System.err.println("    : " + e.getMessage());
            System.exit(1);
        }
		
		/*DEBUGA*/
		System.out.println("expReplaySize: " + expReplaySize);
		System.out.println("batchSize: " + batchSize);
		System.out.println("gamma: " + gamma);
		System.out.println("nnLearningRate: " + nnLearningRate);
		System.out.println("modelSaveFreq: " + modelSaveFreq);
		System.out.println("errorClamp: " + errorClamp);
		System.out.println("hiddenLayers: " + hiddenLayers + " hiddenLayersNeurons: " + 
							Arrays.toString(hiddenLayersNeurons));		
	}
	
	
	/*NN has bias by default*/
	 public MultiLayerConfiguration createConf() {
		 ListBuilder conf = new NeuralNetConfiguration.Builder()
	                .seed(BotServer.SEED)
	                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
	                .weightInit(WeightInit.XAVIER)
//	                .updater(new Nesterovs(0.002, 0.9))
//	                .l2(0.001)
	                .updater(new Adam(nnLearningRate))
	                .list()
	                .layer(0, new DenseLayer.Builder().nIn(inputsize).nOut(hiddenLayersNeurons[0])
	                        .weightInit(WeightInit.XAVIER)
	                        .activation(Activation.RELU)
	                        .build());

		 /*build each hidden layer*/
       for (int i=1; i<hiddenLayers; i++) {
    	   conf.layer(i, new DenseLayer.Builder().nIn(hiddenLayersNeurons[i-1]).nOut(hiddenLayersNeurons[i])
                   .weightInit(WeightInit.XAVIER)
                   .activation(Activation.RELU)
                   .build());
       }	                
	                
        conf.layer(hiddenLayers, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
        		.weightInit(WeightInit.XAVIER)
//	                        .activation(Activation.IDENTITY)
                .activation(outputLayerActivation)
                .nIn(hiddenLayersNeurons[hiddenLayersNeurons.length-1]).nOut(1).build());
		 
		 return conf.build();
	}

	
	@Override
	public void run() {
		/*DEBUGA*/
		System.out.println("bot " + id + " started running");
		
		while(expReplay.size()<batchSize) {
			try
        	{
        	    Thread.sleep(500);
        	}
        	catch(InterruptedException ex)
        	{
        	    Thread.currentThread().interrupt();
        	}			
//			/*DEBUG*/
//			System.out.println("bot" + id + " expReplay.size(): "+ expReplay.size() + "batchSize: "+ batchSize);
		}
		
//		/*DEBUG*/
//		System.out.println("expReplay.size(): "+ expReplay.size() + "batchSize: "+ batchSize);
		
		while(isTraining) {
//			/*DEBUG*/
//			System.out.println("epoch " + epoch);
			counter++;
			
			for (int step=0; step<200; step++) {
				Pair<INDArray, INDArray> targets = setTarget(getBatch());
	            currentNetwork.fit(targets.getFirst(), targets.getSecond());
			}
			
            try
        	{
        	    Thread.sleep(1000);
        	}
        	catch(InterruptedException ex)
        	{
        	    Thread.currentThread().interrupt();
        	}
				
			
			targetNetwork = currentNetwork.clone();
			
//			if ((counter % modelSaveFreq)==0) {
////				writeMemory("player" + id);
//			}
			
		}
		
		/*DEBUGA*/
		System.out.println("bot" + id + " epoch " + counter + 
				" expReplay.size(): " + expReplay.size());
	}
	
	/*needed for backward compatibility, as in RLClinet method like this from 
	 * StateMemoryLookupTable is used
	 */
	public void writeMemory(String name, boolean org) {
		writeMemory(name);
	}
	
	public void writeMemory(String name) {
		System.out.println("Writing NN");
		try {
			ModelSerializer.writeModel(currentNetwork, name, true);
		}
		catch (IOException exp) {
			exp.printStackTrace();
		}
	}
	
	public void readMemory(String name) {
		System.out.println("Reading NN");
		try {
			currentNetwork = ModelSerializer.restoreMultiLayerNetwork(name);
			System.out.println(currentNetwork.summary());
		}
		catch (IOException exp) {
			exp.printStackTrace();
		}
	}
	
	public float getStateValue(float[] stateArr) {
		INDArray input = Nd4j.create(stateArr); // preprocessed and normalized in SOCPlayerStateNN
		return getStateValue(input);
	}
	
	public float getStateValue(INDArray input) {
		INDArray output = currentNetwork.output(input);
		return output.getFloat(0); //make sure you get a single output
	}
	
	public ArrayList< StateTransition > getBatch(int size) {
        ArrayList<StateTransition> batch = new ArrayList<>(size);
        int storageSize = 0;
        synchronized (expReplay) {
        	storageSize = expReplay.size();
        }
        int actualBatchSize = Math.min(storageSize, size);
        
        int[] actualIndex = rnd.ints(0, storageSize).distinct().limit(actualBatchSize).toArray();

//        /*DEBUG*/
//        System.out.println("storage size: " + storageSize + " batchsize: " + actualBatchSize + 
//        		" indexes: " + Arrays.toString(actualIndex));
        
        int ind = 0;
        try {
        	 for (int i = 0; i < actualBatchSize; i ++) {
        		ind = actualIndex[i];
        		StateTransition trans = null;
        		synchronized (expReplay) {
//        			System.out.println("bot" + id + " ind: " + ind + " expReplay.size(): " + expReplay.size()); 
        			trans = expReplay.get(ind);
        		}
             	//not sure if duplication is needed here, left just in case
                 batch.add(trans);
             }
        } catch (Exception exp) {
        	exp.printStackTrace();
        	/*DEBUGA*/
        	System.out.println("bot" + id + " epoch " + counter + 
    				" expReplay.size(): " + expReplay.size() + " ind: " + ind);  
        	System.out.println(currentNetwork.summary());
//        	StateTransition trans = expReplay.get(ind);
//        	System.out.println("trans taken succesfully");
        }
        return batch;
    }
	
	public ArrayList< StateTransition > getBatch() {
		return getBatch(batchSize);
	}
	
	public void store( StateTransition transition) {
		synchronized (expReplay) {
			expReplay.add(transition);
		}
        //log.info("size: "+storage.size());
    }
	
	public void store(float[] state, float[] nextState, double reward) {
		INDArray st = Nd4j.create(state);
		INDArray nst = Nd4j.create(nextState);
		synchronized (expReplay) {
			expReplay.add(new StateTransition(st, nst, reward));
		}
//		trainStep();
		
//        log.info("size: "+storage.size());
//		/*DEBUG*/
//		System.out.println("bot" + id + " expReplay size " + expReplay.size());
    }
	
	public void store(INDArray state, INDArray nextState, double reward) {
		synchronized (expReplay) {
			expReplay.add(new StateTransition(state, nextState, reward));
		}
		
	}
	
	protected Pair<INDArray, INDArray> setTarget(ArrayList< StateTransition > transitions) {
        if (transitions.size() == 0)
            throw new IllegalArgumentException("too few transitions");

        int rows = transitions.size();
        int cols = transitions.get(0).getState().columns();

        INDArray obs = Nd4j.create(rows, cols);
        INDArray nextObs = Nd4j.create(rows, cols);

        for (int i = 0; i < rows; i++) {
        	StateTransition trans = transitions.get(i);
        	obs.putRow(i, trans.getState());      	
        	nextObs.putRow(i,  trans.getNextState());
        }

        INDArray dqnOutputAr = currentNetwork.output(obs);
        INDArray targetDqnOutputNext = targetNetwork.output(nextObs);

        for (int i = 0; i < rows; i++) {
            double yTar = transitions.get(i).getReward();
            if (yTar<1) {
                yTar += gamma * targetDqnOutputNext.getDouble(0);
            }

            double previousV = dqnOutputAr.getDouble(i);
            double lowB = previousV - errorClamp;
            double highB = previousV + errorClamp;
            double clamped = Math.min(highB, Math.max(yTar, lowB));

            dqnOutputAr.putScalar(i, clamped);
        }

        return new Pair<INDArray, INDArray>(obs, dqnOutputAr);
    }
	
	class StateTransition {
		INDArray state;
		INDArray nextState;
		double reward;
		
		public StateTransition(INDArray state, INDArray nextState, double reward) {
			this.state=state;
			this.nextState=nextState;
			this.reward=reward;
		}
		
		public INDArray getState() {
			return state;
		}
		
		public INDArray getNextState() {
			return nextState;
		}
		
		public double getReward() {
			return reward;
		}
		
		
	}

	public void memoryStats() {
		// TODO Auto-generated method stub
		
	}
	
	protected void trainStep() {
		if (expReplay.size()>batchSize) {
			counter++;
			Pair<INDArray, INDArray> targets = setTarget(getBatch());
	        currentNetwork.fit(targets.getFirst(), targets.getSecond());
	        
	        if ((counter%200)==0) {
	        	targetNetwork = currentNetwork.clone();
	        }
		}
	}
	
	public void printCounter() {
		System.out.println("bot" + id + " counter: " + counter);
	}
	
	public void printStats() {
		/*DEBUGA*/
    	System.out.println("bot" + id + " epoch " + counter + 
				" expReplay.size(): " + expReplay.size());  
    	System.out.println(currentNetwork.summary());
	}
	
	public boolean getIsTraining() {
		return isTraining;
	}


}
