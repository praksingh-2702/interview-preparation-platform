import React from 'react';

const Brand = ({ as: Tag = 'span', className = '' }) => (
    <Tag className={`brand ${className}`}>
        Freak<span className="brand-accent">Code</span>
    </Tag>
);

export default Brand;
