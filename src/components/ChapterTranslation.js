import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function ChapterTranslation() {
  const [chapters, setChapters] = useState([]);
  const [selectedChapter, setSelectedChapter] = useState('');
  const [translation, setTranslation] = useState('');
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    // Fetch the XML from backend and parse chapters
    fetch('http://localhost:8080/api/pdf/xml')
      .then(response => response.text())
      .then(xmlString => {
        // Parse XML and extract chapters
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(xmlString, 'text/xml');
        // Assuming chapters are elements named 'chapter' with attribute or text content
        const chapterElements = xmlDoc.getElementsByTagName('chapter');
        const chapterList = [];
        for (let i = 0; i < chapterElements.length; i++) {
          const chapter = chapterElements[i];
          // Use attribute 'title' or text content as chapter name
          const title = chapter.getAttribute('title') || chapter.textContent || `Chapter ${i+1}`;
          chapterList.push(title);
        }
        setChapters(chapterList);
        if (chapterList.length > 0) {
          setSelectedChapter(chapterList[0]);
        }
      })
      .catch(error => {
        console.error('Error fetching or parsing XML:', error);
      });
  }, []);

  const handleChapterChange = (e) => {
    setSelectedChapter(e.target.value);
    // TODO: Implement translation fetch or logic for selected chapter
    setTranslation(`Translation for ${e.target.value} will be shown here.`);
  };

  const handleExport = async () => {
    if (!selectedChapter) {
      setMessage('Please select a chapter to export.');
      return;
    }
    try {
      const response = await fetch('http://localhost:8080/api/pdf/exportChapter', {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: selectedChapter,
      });
      if (response.ok) {
        // Handle PDF download
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = selectedChapter + '_translated.pdf';
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
        setMessage('Chapter exported and PDF downloaded successfully.');
      } else {
        const errorText = await response.text();
        setMessage('Error: ' + errorText);
      }
    } catch (error) {
      setMessage('Error exporting chapter: ' + error.message);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100 p-4">
      <button
        onClick={() => navigate(-1)}
        className="mb-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
      >
        Back
      </button>
      <h1 className="text-2xl font-semibold mb-4">Chapter Translation</h1>
      <select
        value={selectedChapter}
        onChange={handleChapterChange}
        className="mb-4 p-2 border border-gray-300 rounded"
      >
        {chapters.map((chapter, index) => (
          <option key={index} value={chapter}>
            {chapter}
          </option>
        ))}
      </select>
      <div className="mb-4">
        <button
          onClick={handleExport}
          className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition"
          title="Export Translated Chapter XML and PDF"
        >
          Export Translated Chapter XML and PDF
        </button>
      </div>
      {message && (
        <div className="mb-4 p-2 border border-gray-300 rounded bg-yellow-100 text-yellow-800 max-w-xl">
          {message}
        </div>
      )}
      <div className="p-4 border border-gray-300 rounded bg-white w-full max-w-xl">
        {translation}
      </div>
    </div>
  );
}
