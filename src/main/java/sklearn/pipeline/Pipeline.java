/*
 * Copyright (c) 2016 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn.pipeline;

import java.util.List;

import com.google.common.collect.Lists;
import org.jpmml.sklearn.CastFunction;
import org.jpmml.sklearn.Castable;
import org.jpmml.sklearn.ClassDictUtil;
import org.jpmml.sklearn.TupleUtil;
import sklearn.Composite;
import sklearn.Estimator;
import sklearn.PassThrough;
import sklearn.Transformer;

public class Pipeline extends Composite implements Castable {

	public Pipeline(){
		this("sklearn.pipeline", "Pipeline");
	}

	public Pipeline(String module, String name){
		super(module, name);
	}

	@Override
	public boolean hasTransformers(){
		List<Object[]> steps = getSteps();

		if(steps.size() < 1){
			return false;
		} else

		if(steps.size() == 1){
			return !hasFinalEstimator();
		} else

		{
			return true;
		}
	}

	@Override
	public boolean hasFinalEstimator(){
		List<Object[]> steps = getSteps();

		if(steps.size() < 1){
			return false;
		}

		Object[] finalStep = steps.get(steps.size() - 1);

		Object estimator = TupleUtil.extractElement(finalStep, 1);

		return ("passthrough").equals(estimator) || Estimator.class.isInstance(estimator);
	}

	@Override
	public List<? extends Transformer> getTransformers(){
		List<Object[]> steps = getSteps();

		if(hasFinalEstimator()){
			steps = steps.subList(0, steps.size() - 1);
		}

		List<?> transformers = TupleUtil.extractElementList(steps, 1);

		CastFunction<Transformer> castFunction = new CastFunction<Transformer>(Transformer.class){

			@Override
			public Transformer apply(Object object){

				if(("passthrough").equals(object)){
					return PassThrough.INSTANCE;
				}

				return super.apply(object);
			}

			@Override
			public String formatMessage(Object object){
				return "The transformer object (" + ClassDictUtil.formatClass(object) + ") is not a supported Transformer";
			}
		};

		return Lists.transform(transformers, castFunction);
	}

	@Override
	public Estimator getFinalEstimator(){
		List<Object[]> steps = getSteps();

		if(steps.size() < 1){
			throw new IllegalArgumentException("Expected one or more steps, got zero steps");
		}

		Object[] finalStep = steps.get(steps.size() - 1);

		Object estimator = TupleUtil.extractElement(finalStep, 1);

		CastFunction<Estimator> castFunction = new CastFunction<Estimator>(Estimator.class){

			@Override
			public Estimator apply(Object object){

				if(("passthrough").equals(object)){
					return null;
				}

				return super.apply(object);
			}

			@Override
			public String formatMessage(Object object){
				return "The transformer object of the final step (" + ClassDictUtil.formatClass(object) + ") is not a supported Estimator";
			}
		};

		return castFunction.apply(estimator);
	}

	@Override
	public Object castTo(Class<?> clazz){

		if((Transformer.class).equals(clazz)){
			return toTransformer();
		} else

		if((Estimator.class).equals(clazz)){
			return toEstimator();
		}

		return this;
	}

	public Transformer toTransformer(){

		if(hasFinalEstimator()){
			Estimator estimator = getFinalEstimator();

			if(estimator != null){
				throw new IllegalArgumentException("The pipeline ends with an estimator object");
			}
		}

		return new PipelineTransformer(this);
	}

	public Estimator toEstimator(){
		return new PipelineEstimator(this);
	}

	public List<Object[]> getSteps(){
		return getTupleList("steps");
	}
}