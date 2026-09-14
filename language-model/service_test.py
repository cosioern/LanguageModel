import os
os.environ["MOCK_MODE"] = "true"  # must be set before importing microservice

import io
from fastapi.testclient import TestClient
from microservice import app

client = TestClient(app, raise_server_exceptions=False)


class TestGenerateEndpoint:
    def test_generate_returns_mock_response(self):
        payload = {"messages": [{"role": "user", "content": "Hello"}]}
        res = client.post("/generate", json=payload)
        assert res.status_code == 200
        assert "This is a mock response for the prompt: Hello" in res.text

    def test_generate_missing_content_field_returns_422(self):
        res = client.post("/generate", json={"messages": [{"role": "user"}]})
        assert res.status_code == 422

    def test_generate_empty_messages_list(self):
        # req.messages[-1] on an empty list raises IndexError -> 500
        res = client.post("/generate", json={"messages": []})
        assert res.status_code == 500


class TestEmbedDocumentEndpoint:
    def test_embed_txt_file(self):
        res = client.post(
            "/embedDocument",
            files={"file": ("test.txt", io.BytesIO(b"sample text"), "text/plain")},
        )
        assert res.status_code == 200
        data = res.json()
        assert data[0]["content"] == "A mock for an embedded chunk."
        assert data[0]["embedding"] == [0.0] * 384

    def test_embed_pdf_file(self, mocker):
        mocker.patch("microservice.pymupdf.open", return_value=["page text"])
        mocker.patch("microservice.SentenceSplitter.split_text", return_value=["chunk"])
        mocker.patch("microservice.encoder_model", create=True)
        mocker.patch("microservice.encoder_model.encode", create=True, return_value=[[0.5]])
        res = client.post(
            "/embedDocument",
            files={"file": ("test.pdf", io.BytesIO(b"%PDF-1.4 fake"), "application/pdf")},
        )
        assert res.status_code == 200

    def test_embed_docx_file(self, mocker):
        mocker.patch("microservice.Document")
        mocker.patch("microservice.SentenceSplitter.split_text", return_value=["chunk"])
        mocker.patch("microservice.encoder_model", create=True)
        mocker.patch("microservice.encoder_model.encode", create=True, return_value=[[0.5]])
        res = client.post(
            "/embedDocument",
            files={"file": ("test.docx", io.BytesIO(b"fake docx"),
                             "application/vnd.openxmlformats-officedocument.wordprocessingml.document")},
        )
        assert res.status_code == 200

    def test_embed_unsupported_file_type_returns_400(self):
        res = client.post(
            "/embedDocument",
            files={"file": ("test.xyz", io.BytesIO(b"data"), "application/octet-stream")},
        )
        assert res.status_code == 400
        assert res.json()["detail"] == "Unsupported File Type"


class TestEmbedPromptEndpoint:
    def test_embed_prompt_mock_mode_returns_zero_vector(self):
        res = client.post("/embedPrompt", json={"role": "user", "content": "test prompt"})
        assert res.status_code == 200
        assert res.json() == [0.0] * 384

    def test_embed_prompt_missing_content_returns_422(self):
        res = client.post("/embedPrompt", json={"role": "user"})
        assert res.status_code == 422