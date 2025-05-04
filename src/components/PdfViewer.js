import React, { useState, useRef, useEffect } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { useNavigate } from 'react-router-dom';
import 'react-pdf/dist/esm/Page/AnnotationLayer.css';

pdfjs.GlobalWorkerOptions.workerSrc = '//cdnjs.cloudflare.com/ajax/libs/pdf.js/' + pdfjs.version + '/pdf.worker.min.js';

export default function PdfViewer({ file, xmlContent, onAnnotationAdded }) {
  const [numPages, setNumPages] = useState(null);
  const [scale, setScale] = useState(1.0);
  const [highlights, setHighlights] = useState([]);
  const [isHighlighting, setIsHighlighting] = useState(false);
  const [isErasing, setIsErasing] = useState(false);
  const [isAddingTextBox, setIsAddingTextBox] = useState(false);
  const [textBoxes, setTextBoxes] = useState([]);
  const [hyperlinkUrl, setHyperlinkUrl] = useState('');
  const [showHyperlinkInput, setShowHyperlinkInput] = useState(false);
  const [selectedTextId, setSelectedTextId] = useState(null);
  const navigate = useNavigate();  
  const [draggingId, setDraggingId] = useState(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [loading, setLoading] = useState(false);
  const containerRef = useRef(null);
  const canvasRef = useRef(null);
  const textBoxIdCounter = useRef(0);

  const handleExportClick = async () => {
    if (!xmlContent) {
      alert('No XML content to export');
      return;
    }
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/pdf/export', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/xml',
        },
        body: xmlContent,
      });
      if (!response.ok) {
        throw new Error('Export failed');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'exported.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('Error exporting PDF: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleExportTranslatedClick = async () => {
    if (!xmlContent) {
      alert('No XML content to export');
      return;
    }
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/pdf/exportTranslated', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/xml',
        },
        body: xmlContent,
      });
      if (!response.ok) {
        throw new Error('Export translated PDF failed');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'translated_output.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('Error exporting translated PDF: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleExportToHtmlClick = async () => {
    if (!xmlContent) {
      alert('No XML content to export');
      return;
    }
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/pdf/exportToHtml', {
        method: 'POST',
      });
      if (!response.ok) {
        throw new Error('Export to HTML failed');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'output.html';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('Error exporting to HTML: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  function onDocumentLoadSuccess({ numPages }) {
    setNumPages(numPages);
  }

  const zoomIn = () => {
    setScale((prev) => Math.min(prev + 0.25, 3));
  };

  const zoomOut = () => {
    setScale((prev) => Math.max(prev - 0.25, 0.5));
  };

  // Highlighting and erasing logic
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    highlights.forEach((highlight) => {
      if (highlight.page === 1) {
        ctx.fillStyle = 'rgba(255, 255, 0, 0.4)';
        ctx.fillRect(highlight.x, highlight.y, highlight.width, highlight.height);
      }
    });
  }, [highlights]);

  const handleMouseDown = (e) => {
    if (isAddingTextBox) {
      const rect = e.currentTarget.getBoundingClientRect();
      const x = (e.clientX - rect.left) / scale;
      const y = (e.clientY - rect.top) / scale;
      const newTextBox = {
        id: textBoxIdCounter.current++,
        page: 1,
        x,
        y,
        width: 150,
        height: 50,
        text: '',
      };
      setTextBoxes((prev) => [...prev, newTextBox]);
      setIsAddingTextBox(false);
      return;
    }
    if (!isHighlighting && !isErasing) return;
    const rect = e.target.getBoundingClientRect();
    const startX = e.clientX - rect.left;
    const startY = e.clientY - rect.top;
    let currentHighlight = { x: startX, y: startY, width: 0, height: 0, page: 1 };

    const handleMouseMove = (eMove) => {
      const currentX = eMove.clientX - rect.left;
      const currentY = eMove.clientY - rect.top;
      currentHighlight.width = currentX - startX;
      currentHighlight.height = currentY - startY;
      drawHighlights(currentHighlight);
    };

    const handleMouseUp = (eUp) => {
      if (isHighlighting) {
        setHighlights((prev) => [...prev, currentHighlight]);
      } else if (isErasing) {
        // Erase highlights that intersect with currentHighlight
        setHighlights((prev) =>
          prev.filter(
            (h) =>
              !(
                h.page === 1 &&
                rectanglesIntersect(h, currentHighlight)
              )
          )
        );
      }
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
      clearCanvas();
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
  };

  const drawHighlights = (tempHighlight) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    highlights.forEach((highlight) => {
      if (highlight.page === 1) {
        ctx.fillStyle = 'rgba(255, 255, 0, 0.4)';
        ctx.fillRect(highlight.x, highlight.y, highlight.width, highlight.height);
      }
    });

    if (tempHighlight) {
      ctx.fillStyle = 'rgba(255, 255, 0, 0.4)';
      ctx.fillRect(tempHighlight.x, tempHighlight.y, tempHighlight.width, tempHighlight.height);
    }
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  };

  const rectanglesIntersect = (r1, r2) => {
    return !(
      r2.x > r1.x + r1.width ||
      r2.x + r2.width < r1.x ||
      r2.y > r1.y + r1.height ||
      r2.y + r2.height < r1.y
    );
  };

  // Drag handlers for text boxes
  const handleTextBoxMouseDown = (e, id) => {
    e.stopPropagation();
    const rect = e.currentTarget.getBoundingClientRect();
    setDraggingId(id);
    setDragOffset({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    });
  };

  const handleMouseMove = (e) => {
    if (draggingId === null) return;
    const containerRect = containerRef.current.getBoundingClientRect();
    const x = (e.clientX - containerRect.left - dragOffset.x) / scale;
    const y = (e.clientY - containerRect.top - dragOffset.y) / scale;
    setTextBoxes((prev) =>
      prev.map((tb) =>
        tb.id === draggingId ? { ...tb, x: x < 0 ? 0 : x, y: y < 0 ? 0 : y } : tb
      )
    );
  };

  const handleMouseUp = () => {
    setDraggingId(null);
  };

  const handleTextChange = async (e, id) => {
    const newText = e.target.innerText;
    setTextBoxes((prev) =>
      prev.map((tb) => (tb.id === id ? { ...tb, text: newText } : tb))
    );

    // Call backend to add annotation
    try {
      const response = await fetch('http://localhost:8080/api/pdf/annotate', {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: newText,
      });
      if (!response.ok) {
        console.error('Failed to add annotation');
      } else {
        if (onAnnotationAdded) {
          onAnnotationAdded();
        }
      }
    } catch (error) {
      console.error('Error adding annotation:', error);
    }
  };

  useEffect(() => {
    if (draggingId !== null) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    } else {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingId]);

  return (
    <>
      <div className="flex flex-col items-center">
        <div className="mb-4 flex space-x-4">
          <button
            onClick={zoomOut}
            className="px-3 py-1 bg-gray-300 rounded hover:bg-gray-400 transition"
            title="Zoom Out"
          >
            <i className="fas fa-search-minus"></i>
          </button>
          <button
            onClick={zoomIn}
            className="px-3 py-1 bg-gray-300 rounded hover:bg-gray-400 transition"
            title="Zoom In"
          >
            <i className="fas fa-search-plus"></i>
          </button>
          <button
            onClick={() => {
              setIsHighlighting(!isHighlighting);
              setIsErasing(false);
              setIsAddingTextBox(false);
            }}
            className={`px-3 py-1 rounded transition ${
              isHighlighting ? 'bg-yellow-400 text-black' : 'bg-gray-300 hover:bg-gray-400'
            }`}
            title="Highlight"
          >
            <i className="fas fa-highlighter"></i>
          </button>
          <button
            onClick={handleExportClick}
            disabled={loading}
            className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            title="Export to PDF"
          >
            {loading ? 'Exporting...' : 'Export to PDF'}
          </button>
          <button
            onClick={handleExportTranslatedClick}
            disabled={loading}
            className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
            title="Export Translated PDF"
          >
            {loading ? 'Exporting...' : 'Export Translated PDF'}
          </button>
          <button
            onClick={handleExportToHtmlClick}
            disabled={loading}
            className="px-3 py-1 bg-purple-600 text-white rounded hover:bg-purple-700 disabled:opacity-50"
            title="Export to HTML"
          >
            {loading ? 'Exporting...' : 'Export to HTML'}
          </button>
          <button
            onClick={() => {
              setIsErasing(!isErasing);
              setIsHighlighting(false);
              setIsAddingTextBox(false);
            }}
            className={`px-3 py-1 rounded transition ${
              isErasing ? 'bg-red-400 text-white' : 'bg-gray-300 hover:bg-gray-400'
            }`}
            title="Erase"
          >
            <i className="fas fa-eraser"></i>
          </button>
          <button
            onClick={() => {
              setIsAddingTextBox(!isAddingTextBox);
              setIsHighlighting(false);
              setIsErasing(false);
            }}
            className={`px-3 py-1 rounded transition ${
              isAddingTextBox ? 'bg-green-400 text-black' : 'bg-gray-300 hover:bg-gray-400'
            }`}
            title="Add Text Box"
          >
            <i className="fas fa-font"></i>
          </button>
          <button
            onClick={() => {
              if (!xmlContent) {
                alert('No XML content available for chapter-wise translation');
                return;
              }
              navigate('/translate', { state: { xml: xmlContent } });
            }}
            className="px-3 py-1 bg-purple-600 text-white rounded hover:bg-purple-700 transition"
            title="Translate Chapter Wise"
          >
            Translate Chapter Wise
          </button>
        </div>
        <div
          ref={containerRef}
          className="relative border border-gray-300 shadow-lg overflow-y-auto"
          style={{ width: 600 * scale, height: 800 * scale * 2 }}
          onMouseDown={handleMouseDown}
        >
          <Document
            file={file}
            onLoadSuccess={onDocumentLoadSuccess}
            onLoadError={(error) => {
              console.error('Error while loading PDF:', error);
              alert('Failed to load PDF file. Please try another file.');
            }}
          >
            {Array.from(new Array(numPages), (el, index) => (
              <Page key={`page_${index + 1}`} pageNumber={index + 1} scale={scale} renderTextLayer={false} />
            ))}
          </Document>
          <canvas
            ref={canvasRef}
            width={600 * scale}
            height={800 * scale * 2}
            className="absolute top-0 left-0 pointer-events-none"
          />
          {textBoxes
            .filter((tb) => tb.page === 1)
            .map((tb) => (
              <div
                key={tb.id}
                className="absolute bg-white border border-gray-400 rounded p-1 shadow resize overflow-auto"
                style={{
                  top: tb.y * scale,
                  left: tb.x * scale,
                  width: tb.width * scale,
                  height: tb.height * scale,
                  cursor: draggingId === tb.id ? 'grabbing' : 'grab',
                  userSelect: draggingId === tb.id ? 'none' : 'text',
                  outline: 'none',
                  zIndex: 10,
                }}
              >
                <button
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setTextBoxes((prev) => prev.filter((box) => box.id !== tb.id));
                  }}
                  className="absolute top-0 right-0 m-1 text-red-600 hover:text-red-800 bg-white rounded-full w-5 h-5 flex items-center justify-center text-xs font-bold"
                  title="Delete Text Box"
                >
                  &times;
                </button>
                <div
                  contentEditable
                  suppressContentEditableWarning
                  onMouseDown={(e) => handleTextBoxMouseDown(e, tb.id)}
                  onBlur={(e) => {
                    // Update text and size on blur to save changes
                    handleTextChange(e, tb.id);
                    const el = e.currentTarget;
                    const newWidth = el.offsetWidth / scale;
                    const newHeight = el.offsetHeight / scale;
                    setTextBoxes((prev) =>
                      prev.map((box) =>
                        box.id === tb.id ? { ...box, width: newWidth, height: newHeight } : box
                      )
                    );
                  }}
                  className="w-full h-full outline-none"
                  style={{
                    textAlign: 'start',
                    direction: 'ltr',
                    unicodeBidi: 'plaintext',
                    fontFamily: 'Arial, sans-serif',
                    whiteSpace: 'pre-wrap',
                    cursor: draggingId === tb.id ? 'grabbing' : 'text',
                    userSelect: draggingId === tb.id ? 'none' : 'text',
                  }}
                >
                  {tb.text}
                </div>
              </div>
            ))}
        </div>
      </div>
    </>
  );
}
