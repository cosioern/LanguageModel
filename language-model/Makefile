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

microservice:
	uvicorn main:app --host 0.0.0.0 --port 8000	