PYTHON = python

main:
	$(PYTHON) parser.py
	$(PYTHON) classify.py

infer:
	$(PYTHON) evaluate.py

train:
	$(PYTHON) train.py

microservice:
	uvicorn main:app --host 0.0.0.0 --port 8000