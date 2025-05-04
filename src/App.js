import React, { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import PdfViewer from './components/PdfViewer';
import ChapterTranslation from './components/ChapterTranslation';

export default function App() {
  const [file, setFile] = useState(null);
  const [xmlResult, setXmlResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleFileChange = async (e) => {
    const selectedFile = e.target.files[0];
    if (!selectedFile || selectedFile.type !== 'application/pdf') {
      alert('Please select a valid PDF file.');
      return;
    }

    setLoading(true);
    setError(null);
    setXmlResult(null);

    const formData = new FormData();
    formData.append('pdf', selectedFile);

    try {
      const response = await fetch('http://localhost:8080/api/pdf/convert', {
        method: 'POST',
        body: formData,
      });
      if (!response.ok) {
        const errorText = await response.text();
        setError(errorText || 'Conversion failed');
        setLoading(false);
        return;
      }
      const data = await response.text();
      setXmlResult(data);
      setFile(selectedFile);

      // Fetch updated XML from backend to ensure sync
      const xmlResponse = await fetch('http://localhost:8080/api/pdf/xml');
      if (xmlResponse.ok) {
        const updatedXml = await xmlResponse.text();
        setXmlResult(updatedXml);
      } else {
        console.error('Failed to fetch updated XML');
      }
    } catch (err) {
      setError('Error uploading file');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setFile(null);
    setXmlResult(null);
    setError(null);
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            !file ? (
              <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100 p-4">
                <div className="w-full max-w-md bg-white rounded shadow p-6">
                  <h1 className="text-2xl font-semibold mb-4 text-center">Upload PDF File</h1>
                  <input
                    type="file"
                    accept="application/pdf"
                    onChange={handleFileChange}
                    className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4
                      file:rounded file:border-0
                      file:text-sm file:font-semibold
                      file:bg-blue-50 file:text-blue-700
                      hover:file:bg-blue-100
                    "
                  />
                  {loading && <p className="mt-4 text-center">Converting PDF to XML...</p>}
                  {error && <p className="mt-4 text-center text-red-600">{error}</p>}
                </div>
              </div>
            ) : (
              <div className="w-full h-full max-w-4xl">
                <button
                  onClick={handleReset}
                  className="mb-4 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition"
                >
                  Back to Upload
                </button>
                {xmlResult && (
                  <div className="mb-4 p-2 border border-gray-300 rounded bg-gray-50 max-h-64 overflow-auto whitespace-pre-wrap">
                    <h2 className="font-semibold mb-2">XML Output:</h2>
                    <pre>{xmlResult}</pre>
                  </div>
                )}
                <PdfViewer
                  file={file}
                  xmlContent={xmlResult}
                  onAnnotationAdded={async () => {
                    try {
                      const xmlResponse = await fetch('http://localhost:8080/api/pdf/xml');
                      if (xmlResponse.ok) {
                        const updatedXml = await xmlResponse.text();
                        setXmlResult(updatedXml);
                      } else {
                        console.error('Failed to fetch updated XML');
                      }
                    } catch (error) {
                      console.error('Error fetching updated XML:', error);
                    }
                  }}
                />
              </div>
            )
          }
        />
        <Route path="/translate" element={<ChapterTranslation />} />
      </Routes>
    </BrowserRouter>
  );
}
  