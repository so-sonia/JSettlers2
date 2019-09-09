package fasttester;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.api.TrainingConfig;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.ArrayType;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class NNfunChecker {

	public NNfunChecker() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {


		 ListBuilder conf = new NeuralNetConfiguration.Builder()
	                .seed(BotServer.SEED)
	                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
	                .weightInit(WeightInit.XAVIER)
//	                .updater(new Nesterovs(0.002, 0.9))
//	                .l2(0.001)
//	                .updater(new Adam(0.001))
	                .list()
	                .layer(0, new DenseLayer.Builder().nIn(2).nOut(2)
	                        .weightInit(WeightInit.XAVIER)
	                        .activation(Activation.IDENTITY)
	                        .hasBias(false)
	                        .build())
	                ;
		 
		 
		 conf.layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
	        		.weightInit(WeightInit.XAVIER)
//		                        .activation(Activation.IDENTITY)
	        		.hasBias(false)
	                .activation(Activation.IDENTITY)
	                .nIn(2).nOut(1).build());
		 
		 MultiLayerNetwork currentNetwork = new MultiLayerNetwork(conf.build()); 
		 currentNetwork.init();
		 System.out.println(currentNetwork.summary());
//		 INDArray input = Nd4j.create(new double[] {0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0.5});
//		 INDArray output = Nd4j.create(new double[] {1.});
		 INDArray input = Nd4j.create(new double[] {2., 3.});
		 INDArray output = Nd4j.create(new double[] {1.});
		 
		 INDArray params = Nd4j.create(new double[] {0.11, 0.21, 0.12, 0.08, 0.14, 0.15 });
		 INDArray traces = Nd4j.create(new double[(int) params.length()]);
		 System.out.println(traces);
		 double lambda = 0.9;
		 currentNetwork.setParams(params);
		 
		 currentNetwork.setLearningRate(0.05);
		 currentNetwork.setLearningRate(0.05);
//		 System.out.println(currentNetwork.getLearningRate(0));
//		 System.out.println(currentNetwork.getLearningRate(1));
//		 System.out.println(currentNetwork.getOptimizer().getClass());
		 
		 /*
		  * Różnica między fit() a calculateGradients(), że w fit() zastosowano już LR, 
		  * a w CG() jeszcze nie
		  */
		 System.out.println(currentNetwork.params());
		 System.out.println(currentNetwork.output(input));
		 System.out.println(currentNetwork.feedForward(input));
//		 currentNetwork.initGradientsView();
		 currentNetwork.calculateGradients(input, output, null, null);
//		 System.out.println(currentNetwork.calculateGradients(input, output, null, null));
//		 System.out.println(currentNetwork.gradie);
//		 currentNetwork.computeGradientAndScore();
//		 System.out.println(currentNetwork.gradientAndScore());
		 traces.muli(lambda).subi(currentNetwork.getFlattenedGradients());
		 System.out.println(traces.mul(output.sub(currentNetwork.output(input))).mul(0.05));
		 System.out.println(currentNetwork.output(input).sub(output));
		 
		 params = currentNetwork.params();
		 params.addi(traces.mul(output.sub(currentNetwork.output(input))).mul(0.05));
		 /*
		  * Array is changed in place, no need to set params to neural network 
		  */

//		 currentNetwork.fit(input, output);

//		 currentNetwork.getLayer(0).backpropGradient(epsilon, workspaceMgr)
		
		 System.out.println(currentNetwork.getLayer(0));
		 System.out.println(currentNetwork.getFlattenedGradients());
		 System.out.println(currentNetwork.getGradientsViewArray());
		 System.out.println(currentNetwork.score());
		 System.out.println(currentNetwork.params());
		 System.out.println(currentNetwork.output(input));
//		 System.out.println(currentNetwork.setParams(params););

	}

}
