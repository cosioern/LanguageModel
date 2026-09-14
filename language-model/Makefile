PYTHON = python

main:
	$(PYTHON) parser.py
	$(PYTHON) classify.py
	$(PYTHON) filter.py

infer:
	$(PYTHON) evaluate.py

train:
	$(PYTHON) train.py

pull:
	$(PYTHON) pullExamples.py

pre:
	$(PYTHON) preprocessor.py

test:
	pytest service_test.py

microservice:
	uvicorn microservice:app --host 0.0.0.0 --port 8000	
